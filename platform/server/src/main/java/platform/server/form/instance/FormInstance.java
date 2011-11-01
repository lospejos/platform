package platform.server.form.instance;

import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.FormEventType;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ContinueAutoActionsClientAction;
import platform.interop.action.ResultClientAction;
import platform.interop.action.StopAutoActionsClientAction;
import platform.interop.exceptions.ComplexQueryException;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.filter.CompareValue;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.filter.RegularFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.listener.FormEventListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.session.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static platform.base.BaseUtils.mergeSet;
import static platform.interop.ClassViewType.*;
import static platform.interop.Order.ADD;
import static platform.interop.Order.DIR;
import static platform.interop.Order.REPLACE;
import static platform.server.form.instance.GroupObjectInstance.*;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends IncrementProps<PropertyInterface> {

    public final T BL;

    public ExprChanges getSession() {
        return session;
    }

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.LM.baseClass.findClassID(classID);
    }

    public Modifier<? extends Changes> update(final ExprChanges sessionChanges) {
        return new IncrementProps<Object>(noUpdate) {
            @Override
            public ExprChanges getSession() {
                return sessionChanges;
            }
        };
    }

    List<Property> incrementTableProps = new ArrayList<Property>();

    private void read(final Property property) throws SQLException {
        PropertyGroup<PropertyInterface> prevGroup = incrementGroups.remove(property);
        if(prevGroup!=null)
            tables.remove(prevGroup);

        read(new PropertyGroup<PropertyInterface>() {
                    public List<PropertyInterface> getKeys() {
                        return new ArrayList<PropertyInterface>(property.interfaces);
                    }

                    public Type.Getter<PropertyInterface> typeGetter() {
                        return (Type.Getter<PropertyInterface>) property.interfaceTypeGetter;
                    }

                    public <PP extends PropertyInterface> Map<PP, PropertyInterface> getPropertyMap(Property<PP> mapProperty) {
                        assert mapProperty == property;
                        Map<PP, PropertyInterface> result = new HashMap<PP, PropertyInterface>();
                        for (PP propertyInterface : mapProperty.interfaces)
                            result.put(propertyInterface, (PP) propertyInterface);
                        return result;
                    }
                }, Collections.singleton(property), BL.LM.baseClass);
    }

    public Set<Property> getUpdateProperties(ExprChanges sessionChanges) {
        return getUpdateProperties(update(sessionChanges));
    }

    public Set<Property> getUpdateProperties(Modifier<? extends Changes> modifier) {
        Set<Property> properties = new HashSet<Property>();
        for (Property<?> updateProperty : getUpdateProperties())
            if (updateProperty.hasChanges(modifier))
                properties.add(updateProperty);
        return properties;
    }

    private final WeakReference<FocusListener<T>> weakFocusListener;
    public FocusListener<T> getFocusListener() {
        return weakFocusListener.get();
    }

    private final WeakReference<CustomClassListener> weakClassListener;
    public CustomClassListener getClassListener() {
        return weakClassListener.get();
    }

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    // для импорта конструктор, объекты пустые
    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), false);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), interactive);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, mapObjects, interactive, null);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive, Set<FilterEntity> additionalFixedFilters) throws SQLException {
        super(session);
        this.entity = entity;
        this.BL = BL;
        this.securityPolicy = securityPolicy;

        instanceFactory = new InstanceFactory(computer);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        for(Property property : BL.getPropertyList())
            if(entity.hintsIncrementTable.contains(property)) // чтобы в лексикографике был список
                incrementTableProps.add(property);

        noUpdate = entity.hintsNoUpdate;

        for (int i = 0; i < entity.groups.size(); i++) {
            GroupObjectInstance groupObject = instanceFactory.getInstance(entity.groups.get(i));
            groupObject.order = i;
            groupObject.setClassListener(classListener);
            groups.add(groupObject);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            treeGroups.add(instanceFactory.getInstance(treeGroup));
        }

        for (PropertyDrawEntity<?> propertyDrawEntity : entity.propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                properties.add(propertyDrawInstance);
            }

        Set<FilterEntity> allFixedFilters = additionalFixedFilters == null
                                            ? entity.fixedFilters
                                            : mergeSet(entity.fixedFilters, additionalFixedFilters);
        for (FilterEntity filterEntity : allFixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupEntity filterGroupEntity : entity.regularFilterGroups) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        for (Entry<OrderEntity<?>, Boolean> orderEntity : entity.fixedOrders.entrySet()) {
            OrderInstance orderInstance = orderEntity.getKey().getInstance(instanceFactory);
            orderInstance.getApplyObject().fixedOrders.put(orderInstance, orderEntity.getValue());
        }

        // в первую очередь ставим на объекты из cache'а
        if (classListener != null) {
            for (GroupObjectInstance groupObject : groups) {
                for (ObjectInstance object : groupObject.objects)
                    if (object.getBaseClass() instanceof CustomClass) {
                        Integer objectID = classListener.getObject((CustomClass) object.getBaseClass());
                        if (objectID != null)
                            groupObject.addSeek(object, session.getDataObject(objectID, ObjectType.instance), false);
                    }
            }
        }

        for (Entry<ObjectEntity, ? extends ObjectValue> mapObject : mapObjects.entrySet()) {
            ObjectInstance instance = instanceFactory.getInstance(mapObject.getKey());
            instance.groupTo.addSeek(instance, mapObject.getValue(), false);
        }

        addObjectOnTransaction(FormEventType.INIT);

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.defaultFilter;
            if (defaultInd >= 0 && defaultInd < filterGroup.filters.size()) {
                setRegularFilter(filterGroup, filterGroup.filters.get(defaultInd));
            }
        }

        Set<GroupObjectInstance> wasOrder = new HashSet<GroupObjectInstance>();
        for (Entry<PropertyDrawEntity<?>, Boolean> entry : entity.defaultOrders.entrySet()) {
            PropertyDrawInstance property = instanceFactory.getInstance(entry.getKey());
            GroupObjectInstance toDraw = property.toDraw;
            Boolean ascending = entry.getValue();

            toDraw.changeOrder(property.propertyObject, wasOrder.contains(toDraw) ? ADD : REPLACE);
            if (!ascending) {
                toDraw.changeOrder(property.propertyObject, DIR);
            }
            wasOrder.add(toDraw);
        }

        if(!interactive) {
            endApply();
            this.mapObjects = mapObjects;
        }
        this.interactive = interactive;
    }

    private Map<ObjectEntity, ? extends ObjectValue> mapObjects = null;

    public boolean areObjectsFounded() {
        assert !interactive;
        for(Entry<ObjectEntity, ? extends ObjectValue> mapObjectInstance : mapObjects.entrySet())
            if(!instanceFactory.getInstance(mapObjectInstance.getKey()).getObjectValue().equals(mapObjectInstance.getValue()))
                return false;
        return true;
    }

    private boolean interactive = true;

    public List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>();
    public List<TreeGroupInstance> treeGroups = new ArrayList<TreeGroupInstance>();

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();

    private Collection<ObjectInstance> objects;

    @ManualLazy
    public Collection<ObjectInstance> getObjects() {
        if (objects == null) {
            objects = new ArrayList<ObjectInstance>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    objects.add(object);
        }
        return objects;
    }

    public void addFixedFilter(FilterEntity newFilter) {
        FilterInstance newFilterInstance = newFilter.getInstance(instanceFactory);
        newFilterInstance.getApplyObject().fixedFilters.add(newFilterInstance);
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //
    public GroupObjectInstance getGroupObjectInstance(int groupID) {
        for (GroupObjectInstance groupObject : groups)
            if (groupObject.getID() == groupID)
                return groupObject;
        return null;
    }

    public ObjectInstance getObjectInstance(int objectID) {
        for (ObjectInstance object : getObjects())
            if (object.getID() == objectID)
                return object;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(int propertyID) {
        for (PropertyDrawInstance property : properties)
            if (property.getID() == propertyID)
                return property;
        return null;
    }

    public RegularFilterGroupInstance getRegularFilterGroup(int groupID) {
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups)
            if (filterGroup.getID() == groupID)
                return filterGroup;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, GroupObjectInstance group) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && (group==null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, null);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property) {
        return getPropertyDraw(property.property);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, GroupObjectInstance group) {
        return getPropertyDraw(property.property, group);
    }



    public void serializePropertyEditorType(DataOutputStream outStream, PropertyDrawInstance<?> propertyDraw, Map<ObjectInstance, DataObject> keys, boolean aggValue) throws SQLException, IOException {
        PropertyObjectInstance<?> change = propertyDraw.getChangeInstance(aggValue, BL, keys);
        if (!propertyDraw.isReadOnly() && securityPolicy.property.change.checkPermission(change.property) &&
                (entity.isActionOnChange(change.property) || change.getValueImplement().canBeChanged(this))) {
            outStream.writeBoolean(false);
            TypeSerializer.serializeType(outStream, change.getEditorType());
        } else {
            outStream.writeBoolean(true);
        }
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void changeGroupObject(GroupObjectInstance group, Scroll changeType) throws SQLException {
        switch (changeType) {
            case HOME:
                group.seek(false);
                break;
            case END:
                group.seek(true);
                break;
        }
    }

    public void expandGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable==null)
            group.expandTable = group.createKeyTable();
        group.expandTable.insertRecord(session.sql, value, true, true);
        group.updated |= UPDATED_EXPANDS;
    }

    public void switchClassView(GroupObjectInstance group) {
        ClassViewType newClassView = switchView(group.curClassView);
        if (group.entity.isAllowedClassView(newClassView)) {
            changeClassView(group, newClassView);
        }
    }

    public void changeClassView(GroupObjectInstance group, ClassViewType show) {

        group.curClassView = show;
        group.updated = group.updated | UPDATED_CLASSVIEW;
    }

    // сстандартные фильтры
    public List<RegularFilterGroupInstance> regularFilterGroups = new ArrayList<RegularFilterGroupInstance>();
    private Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>();

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, int filterId) {
        setRegularFilter(filterGroup, filterGroup.getFilter(filterId));
    }

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, RegularFilterInstance filter) {

        RegularFilterInstance prevFilter = regularFilterValues.get(filterGroup);
        if (prevFilter != null)
            prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null) {
            regularFilterValues.remove(filterGroup);
        } else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    public boolean dataChanged = true;

    private DataObject createObject(ConcreteCustomClass cls) throws SQLException {

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return null;

        return session.addObject(cls, this);
    }

    private void resolveAdd(CustomObjectInstance object, ConcreteCustomClass cls, DataObject addObject) throws SQLException {

        // резолвим все фильтры
        for (FilterInstance filter : object.groupTo.getSetFilters())
            if (!FilterInstance.ignoreInInterface || filter.isInInterface(object.groupTo)) // если ignoreInInterface проверить что в интерфейсе
                filter.resolveAdd(session, this, object, addObject);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        object.changeValue(session, addObject);

        object.groupTo.addSeek(object, addObject, false);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    // добавляет во все
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance && cls.isChild(((CustomObjectInstance) object).baseClass)) {
                resolveAdd((CustomObjectInstance) object, cls, addObject);
            }
        }

        return addObject;
    }

    public DataObject addObject(CustomObjectInstance object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        resolveAdd(object, cls, addObject);

        return addObject;
    }

    public void changeClass(CustomObjectInstance object, DataObject change, int classID) throws SQLException {
        if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
            object.changeClass(session, change, classID);
            dataChanged = true;
        }
    }

    public boolean canChangeClass(CustomObjectInstance object) {
        return securityPolicy.cls.edit.change.checkPermission(object.currentClass);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, boolean aggValue) throws SQLException {
        return changeProperty(property, value, null, false, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, new HashMap<ObjectInstance, DataObject>(), value, executeForm, all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues,
                                             PropertyDrawInstance<?> value, Map<ObjectInstance, DataObject> valueColumnKeys, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, mapDataValues, value.getChangeInstance(aggValue, BL, valueColumnKeys), executeForm, all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues, Object value, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        assert !property.isReadOnly();
        return changeProperty(property.getChangeInstance(aggValue, BL, mapDataValues), value, executeForm, all ? property.toDraw : null);
    }

    @Message("message.form.change.property")
    public List<ClientAction> changeProperty(@ParamMessage PropertyObjectInstance<?> property, Object value, RemoteForm executeForm, GroupObjectInstance groupObject) throws SQLException {
        if (securityPolicy.property.change.checkPermission(property.property)) {
            dataChanged = true;
            return property.execute(session, value instanceof CompareValue? (CompareValue) value : session.getObjectValue(value, property.getType()), this, executeForm, groupObject);
        } else {
            return null;
        }
    }

    public void pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws SQLException {
        List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();
        for (Integer id : propertyIDs) {
            properties.add(getPropertyDraw(id));
        }
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> executeList = groupObject.seekObjects(session.sql, session.env, this, BL.LM.baseClass, table.size());

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < table.size()) {
            executeList.putAll(groupObject.createObjects(session, this, table.size() - availableQuantity));
        }

        for (Map<ObjectInstance, DataObject> key : executeList.keySet()) {
            List<Object> row = table.get(executeList.indexOf(key));
            for (PropertyDrawInstance property : properties) {
                PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();

                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }

                int propertyIndex = properties.indexOf(property);
                if (propertyIndex < row.size() //если вдруг копировали не таблицу - может быть разное кол-во значений в строках
                        && !(propertyObjectInstance.getType() instanceof ActionClass) && !property.isReadOnly()) {
                    dataChanged = true;
                    Object value = row.get(propertyIndex);
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), session, value, this);
                }
            }
        }
    }

    public void pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws SQLException {
        for (Integer propertyId : cells.keySet()) {
            PropertyDrawInstance property = getPropertyDraw(propertyId);
            PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();
            for (Map<Integer, Object> keyIds : cells.get(propertyId)) {
                Map<ObjectInstance, DataObject> key = new HashMap<ObjectInstance, DataObject>();
                for (Integer objectId : keyIds.keySet()) {
                    ObjectInstance objectInstance = getObjectInstance(objectId);
                    key.put(objectInstance, session.getDataObject(keyIds.get(objectId), objectInstance.getType()));
                }
                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }
                if (!(propertyObjectInstance.getType() instanceof ActionClass) && !property.isReadOnly()) {
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), session, value, this);
                    dataChanged = true;
                }
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(new HashMap(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), this), GroupType.SUM, new HashMap());
        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("quant", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        Integer quantity = (Integer) result.getValue(0).get("quant");
        if (quantity != null) {
            return quantity;
        } else {
            return 0;
        }
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, Map<ObjectInstance, DataObject> columnKeys) throws SQLException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();
        Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);

        for (ObjectInstance object : columnKeys.keySet()) {
            keys.put(object, columnKeys.get(object).getExpr());
        }
        Expr expr = GroupExpr.create(new HashMap(), propertyDraw.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), GroupType.SUM, new HashMap());

        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("sum", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        return result.getValue(0).get("sum");
    }

    public Map<List<Object>, List<Object>> groupData(Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toGroup,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toSum,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException {
        GroupObjectInstance groupObject = ((PropertyDrawInstance) toGroup.keySet().toArray()[0]).toDraw;
        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        Map<Object, KeyExpr> keyExprMap = new HashMap<Object, KeyExpr>();
        Map<Object, Expr> exprMap = new HashMap<Object, Expr>();
        for (PropertyDrawInstance property : toGroup.keySet()) {
            int i = 0;
            for (Map<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                keyExprMap.put(property.getsID() + i, new KeyExpr("expr"));
                exprMap.put(property.getsID() + i, property.propertyObject.getExpr(keys, this));
            }
        }

        Query<Object, Object> query = new Query<Object, Object>(keyExprMap);
        Expr exprQuant = GroupExpr.create(exprMap, new ValueExpr(1, IntegerClass.instance), groupObject.getWhere(mapKeys, this), GroupType.SUM, keyExprMap);
        query.and(exprQuant.getWhere());

        int separator = toSum.size();
        int idIndex = 0;
        for (int i = 0; i < toSum.size() + toMax.size(); i++) {
            Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> currentMap;
            int index;
            GroupType groupType;
            if (i < separator) {
                currentMap = toSum;
                groupType = GroupType.SUM;
                index = i;
            } else {
                currentMap = toMax;
                groupType = GroupType.MAX;
                index = i - separator;
            }
            PropertyDrawInstance property = (PropertyDrawInstance) currentMap.keySet().toArray()[index];
            if (property == null) {
                query.properties.put("quant", exprQuant);
                continue;
            }
            for (Map<ObjectInstance, DataObject> columnKeys : currentMap.get(property)) {
                idIndex++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                Expr expr = GroupExpr.create(exprMap, property.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), groupType, keyExprMap);
                query.properties.put(property.getsID() + idIndex, expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<List<Object>, List<Object>>();
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        for (Map<Object, Object> one : result.keyList()) {
            List<Object> groupList = new ArrayList<Object>();
            List<Object> sumList = new ArrayList<Object>();

            for (PropertyDrawInstance propertyDraw : toGroup.keySet()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(propertyDraw.getsID() + i));
                }
            }
            int index = 1;
            for (PropertyDrawInstance propertyDraw : toSum.keySet()) {
                if (propertyDraw == null) {
                    sumList.add(result.get(one).get("quant"));
                    continue;
                }
                for (int i = 1; i <= toSum.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            for (PropertyDrawInstance propertyDraw : toMax.keySet()) {
                for (int i = 1; i <= toMax.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            resultMap.put(groupList, sumList);
        }
        return resultMap;
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
        dataChanged = session.hasChanges();
    }

    void addObjectOnTransaction(FormEventType event) throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance) {
                CustomObjectInstance customObject = (CustomObjectInstance) object;
                if (customObject.isAddOnEvent(event)) {
                    addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
            }
            if (object.isResetOnApply())
                object.groupTo.dropSeek(object);
        }
    }

    public void applyActionChanges(List<ClientAction> actions) throws SQLException {
        synchronizedCommitApply(checkApply(), actions);
    }

    public String checkApply() throws SQLException {
        return session.check(BL);
    }

    public void synchronizedCommitApply(String checkResult, List<ClientAction> actions) throws SQLException {
        if (entity.isSynchronizedApply)
            synchronized (entity) {
                commitApply(checkResult, actions);
            }
        else
            commitApply(checkResult, actions);
    }

    public void commitApply(String checkResult, List<ClientAction> actions) throws SQLException {
        if (checkResult != null) {
            actions.add(new ResultClientAction(checkResult, true));
            actions.add(new StopAutoActionsClientAction());
            session.cleanApply();
            return;
        }

        session.write(BL, actions);
        cleanIncrementTables();

        refreshData();
        addObjectOnTransaction(FormEventType.APPLY);

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        actions.add(new ResultClientAction(ServerResourceBundle.getString("form.instance.changes.saved"), false));
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);
        cleanIncrementTables();

        // пробежим по всем объектам
        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).updateCurrentClass(session);
        addObjectOnTransaction(FormEventType.CANCEL);

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {
    }

    public void changePageSize(GroupObjectInstance groupObject, Integer pageSize) {
        groupObject.setPageSize(pageSize);
    }

    public void gainedFocus() {
        dataChanged = true;
        FocusListener<T> focusListener = getFocusListener();
        if(focusListener!=null)
            focusListener.gainedFocus(this);
    }

    void close() throws SQLException {

        session.incrementChanges.remove(this);
        for (GroupObjectInstance group : groups) {
            if(group.keyTable!=null)
                group.keyTable.drop(session.sql);
            if(group.expandTable!=null)
                group.expandTable.drop(session.sql);
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public ConcreteCustomClass getObjectClass(ObjectInstance object) {

        if (!(object instanceof CustomObjectInstance))
            return null;

        return ((CustomObjectInstance) object).currentClass;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> result = new HashSet<Property>();
        for (PropertyDrawInstance<?> propertyDraw : properties) {
            result.add(propertyDraw.propertyObject.property);
            if (propertyDraw.propertyCaption != null) {
                result.add(propertyDraw.propertyCaption.property);
            }
            if (propertyDraw.propertyFooter != null) {
                result.add(propertyDraw.propertyFooter.property);
            }
            if (propertyDraw.propertyHighlight != null) {
                result.add(propertyDraw.propertyHighlight.property);
            }
        }
        for (GroupObjectInstance group : groups) {
            if (group.propertyHighlight != null) {
                result.add(group.propertyHighlight.property);
            }
            group.fillUpdateProperties(result);
        }
        result.addAll(incrementTableProps);
        return result;
    }

    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects, boolean newSession, boolean interactive) throws SQLException {
        return new FormInstance<T>(form, BL, newSession ? session.createSession() : session, securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, mapObjects, interactive);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance)object).getBaseClass().getDefaultObjectValue());
        else
            object.changeValue(session, value);

        object.groupTo.addSeek(object, value, false);
    }


    public void seekObject(ValueClass cls, ObjectValue value) throws SQLException {

        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                seekObject(object, value);
        }
    }

    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(entity.eventActions.size() > 0) { // дебилизм конечно но пока так
            forceChangeObject(object, value);
        } else {
            object.groupTo.addSeek(object, value, false);
        }
    }

    public List<ClientAction> changeObject(ObjectInstance object, ObjectValue value, RemoteForm form) throws SQLException {
        seekObject(object, value);
        // запускаем все Action'ы, которые следят за этим объектом
        return fireObjectChanged(object, form);
    }

    public void fullRefresh() {
        try {
            refreshData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        dataChanged = session.hasChanges();
    }

    // транзакция для отката при exception'ах
    private class ApplyTransaction {

        private class Group {

            private abstract class Object<O extends ObjectInstance> {
                O object;
                int updated;

                private Object(O object) {
                    this.object = object;
                    updated = object.updated;
                }

                void rollback() {
                    object.updated = updated;
                }
            }

            private class Custom extends Object<CustomObjectInstance> {
                ObjectValue value;
                ConcreteCustomClass currentClass;

                private Custom(CustomObjectInstance object) {
                    super(object);
                    value = object.value;
                    currentClass = object.currentClass;
                }

                void rollback() {
                    super.rollback();
                    object.value = value;
                    object.currentClass = currentClass;
                }
            }

            private class Data extends Object<DataObjectInstance> {
                java.lang.Object value;

                private Data(DataObjectInstance object) {
                    super(object);
                    value = object.value;
                }

                void rollback() {
                    super.rollback();
                    object.value = value;
                }
            }

            GroupObjectInstance group;
            boolean upKeys, downKeys;
            Set<FilterInstance> filters;
            OrderedMap<OrderInstance, Boolean> orders;
            OrderedMap<Map<ObjectInstance, platform.server.logics.DataObject>, Map<OrderInstance, ObjectValue>> keys;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

            NoPropertyTableUsage<ObjectInstance> groupObjectTable;
            NoPropertyTableUsage<ObjectInstance> expandObjectTable;

            private Group(GroupObjectInstance iGroup) {
                group = iGroup;

                filters = new HashSet<FilterInstance>(group.filters);
                orders = new OrderedMap<OrderInstance, Boolean>(group.orders);
                upKeys = group.upKeys;
                downKeys = group.downKeys;
                keys = new OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>(group.keys);
                updated = group.updated;

                for (ObjectInstance object : group.objects)
                    objects.add(object instanceof CustomObjectInstance ? new Custom((CustomObjectInstance) object) : new Data((DataObjectInstance) object));

                groupObjectTable = group.keyTable;
                expandObjectTable = group.expandTable;
            }

            void rollback() throws SQLException {
                group.filters = filters;
                group.orders = orders;
                group.upKeys = upKeys;
                group.downKeys = downKeys;
                group.keys = keys;
                group.updated = updated;

                for (Object object : objects)
                    object.rollback();

                // восстанавливаем ключи в сессии
                if (groupObjectTable == null) {
                    if (group.keyTable != null) {
                        group.keyTable.drop(session.sql);
                        group.keyTable = null;
                    }
                } else {
                    groupObjectTable.writeKeys(session.sql, group.keys.keySet());
                    group.keyTable = groupObjectTable;
                }
                if (expandObjectTable == null) {
                    if (group.expandTable != null) {
                        group.expandTable.drop(session.sql);
                        group.expandTable = null;
                    }
                } else {
                    expandObjectTable.writeKeys(session.sql, group.keys.keySet());
                    group.expandTable = expandObjectTable;
                }
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyDrawInstance, Boolean> isDrawed;
        Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues;

        IdentityHashMap<FormInstance, DataSession.UpdateChanges> incrementChanges;
        IdentityHashMap<FormInstance, DataSession.UpdateChanges> appliedChanges;
        IdentityHashMap<FormInstance, DataSession.UpdateChanges> updateChanges;

        Map<CustomClass, SingleKeyNoPropertyUsage> add;
        Map<CustomClass, SingleKeyNoPropertyUsage> remove;
        Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data;

        SingleKeyPropertyUsage news = null;

        ApplyTransaction() {
            for (GroupObjectInstance group : FormInstance.this.groups)
                groups.add(new Group(group));
            isDrawed = new HashMap<PropertyDrawInstance, Boolean>(FormInstance.this.isDrawed);
            regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>(FormInstance.this.regularFilterValues);

            if (dataChanged) {
                incrementChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.incrementChanges);
                appliedChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.appliedChanges);
                updateChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.updateChanges);

                add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>(session.add);
                remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>(session.remove);
                data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>(session.data);

                news = session.news;
            }
        }

        void rollback() throws SQLException {
            for (Group group : groups)
                group.rollback();
            FormInstance.this.isDrawed = isDrawed;
            FormInstance.this.regularFilterValues = regularFilterValues;

            if (dataChanged) {
                session.incrementChanges = incrementChanges;
                session.appliedChanges = appliedChanges;
                session.updateChanges = updateChanges;

                session.add = add;
                session.remove = remove;
                session.data = data;

                session.news = news;
            }
        }
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isDrawed = new HashMap<PropertyDrawInstance, Boolean>();

    boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, Set<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(PropertyObjectInstance updated, Set<GroupObjectInstance> groupObjects, Collection<Property> changedProps) {
        return dataUpdated(updated, changedProps)
                || groupUpdated(groupObjects, UPDATED_KEYS)
                || objectUpdated(updated, groupObjects);
    }

    private boolean groupUpdated(Collection<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, Collection<Property> changedProps) {
        return updated.dataUpdated(changedProps);
    }

    void applyFilters() {
        for (GroupObjectInstance group : groups)
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
        for (GroupObjectInstance group : groups)
            group.orders = group.getSetOrders();
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private Map<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    @Message("message.form.increment.read.properties")
    private void updateIncrementTableProps(Collection<Property> changedProps) throws SQLException {
        for(Property property : incrementTableProps) // в changedProps могут быть и cancel'ы и новые изменения
            if((refresh || changedProps.contains(property)) && property.hasChanges(this)) // поэтому проверим что hasChanges
                read(property);
    }

    @Message("message.form.update.props")
    private void updateDrawProps(FormChanges result, Set<GroupObjectInstance> keyGroupObjects, @ParamMessage Set<PropertyReaderInstance> propertyList) throws SQLException {
        Query<ObjectInstance, PropertyReaderInstance> selectProps = new Query<ObjectInstance, PropertyReaderInstance>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            NoPropertyTableUsage<ObjectInstance> groupTable = keyGroup.keyTable;
            selectProps.and(groupTable.getWhere(selectProps.mapKeys));
        }

        for (PropertyReaderInstance propertyReader : propertyList) {
            selectProps.properties.put(propertyReader, propertyReader.getPropertyObjectInstance().getExpr(selectProps.mapKeys, this));
        }

        OrderedMap<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> queryResult = selectProps.executeClasses(session.sql, session.env, BL.LM.baseClass);
        for (PropertyReaderInstance propertyReader : propertyList) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = new HashMap<Map<ObjectInstance, DataObject>, ObjectValue>();
            for (Entry<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> resultRow : queryResult.entrySet())
                propertyValues.put(resultRow.getKey(), resultRow.getValue().get(propertyReader));
            result.properties.put(propertyReader, propertyValues);
        }
    }

    @Message("message.form.end.apply")
    public FormChanges endApply() throws SQLException {

        assert interactive;

        ApplyTransaction transaction = new ApplyTransaction();

        final FormChanges result = new FormChanges();

        try {
            // если изменились данные, применяем изменения
            Collection<Property> changedProps;
            Collection<CustomClass> changedClasses = new HashSet<CustomClass>();
            if (dataChanged) {
                changedProps = session.update(this, changedClasses);
            } else {
                changedProps = new ArrayList<Property>();
            }

            updateIncrementTableProps(changedProps);

            GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
            for (GroupObjectInstance group : groups) {
                if (refresh) {
                    //обновляем classViews при refresh
                    result.classViews.put(group, group.curClassView);
                }

                Map<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, session.env, this, BL.LM.baseClass, refresh, result, changedProps, changedClasses);
                if(selectObjects!=null) // то есть нужно изменять объект
                    updateGroupObject = new GroupObjectValue(group, selectObjects);

                if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) {
                    // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                    updateGroupObject.group.update(session, result, updateGroupObject.value);
                    updateGroupObject = null;
                }
            }

            for (Entry<Set<GroupObjectInstance>, Set<PropertyReaderInstance>> entry : BaseUtils.groupSet(getChangedDrawProps(result, changedProps)).entrySet())
                updateDrawProps(result, entry.getKey(), entry.getValue());

        } catch (ComplexQueryException e) {
            transaction.rollback();
            if (dataChanged) { // если изменились данные cancel'им изменения
                cancelChanges();
                FormChanges cancelResult = endApply();
                cancelResult.message = e.getMessage() + ". Изменения будут отменены";
                return cancelResult;
            } else
                throw e;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (SQLException e) {
            transaction.rollback();
            throw e;
        }

        if (dataChanged)
            result.dataChanged = session.hasStoredChanges();

        // сбрасываем все пометки
        for (GroupObjectInstance group : groups) {
            group.userSeeks = null;

            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result;
    }

    private Map<PropertyReaderInstance, Set<GroupObjectInstance>> getChangedDrawProps(FormChanges result, Collection<Property> changedProps) {
        final Map<PropertyReaderInstance, Set<GroupObjectInstance>> readProperties = new HashMap<PropertyReaderInstance, Set<GroupObjectInstance>>();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            Set<GroupObjectInstance> columnGroupGrids = new HashSet<GroupObjectInstance>();
            for (GroupObjectInstance columnGroup : drawProperty.columnGroupObjects)
                if (columnGroup.curClassView == GRID)
                    columnGroupGrids.add(columnGroup);

            Boolean inInterface = null; Set<GroupObjectInstance> drawGridObjects = null;
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                    drawProperty.propertyObject.isInInterface(drawGridObjects = BaseUtils.addSet(columnGroupGrids, drawProperty.toDraw), forceViewType != null)) // в grid'е
                inInterface = true;
            else if (drawProperty.propertyObject.isInInterface(drawGridObjects = columnGroupGrids, false)) // в панели
                inInterface = false;

            Boolean previous = isDrawed.put(drawProperty, inInterface);
            if(inInterface!=null) {
                boolean read = refresh || !inInterface.equals(previous) // если изменилось представление
                        || groupUpdated(drawProperty.columnGroupObjects, UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)
                if(read || propertyUpdated(drawProperty.propertyObject, drawGridObjects, changedProps)) {
                    readProperties.put(drawProperty, drawGridObjects);
                    if(!inInterface) // говорим клиенту что свойство в панели
                        result.panelProperties.add(drawProperty);
                }

                if (drawProperty.propertyCaption != null && (read || propertyUpdated(drawProperty.propertyCaption, columnGroupGrids, changedProps)))
                    readProperties.put(drawProperty.captionReader, columnGroupGrids);
                if (drawProperty.propertyFooter != null && (read || propertyUpdated(drawProperty.propertyFooter, columnGroupGrids, changedProps)))
                    readProperties.put(drawProperty.footerReader, columnGroupGrids);
                if (drawProperty.propertyHighlight != null && (read || propertyUpdated(drawProperty.propertyHighlight, drawGridObjects, changedProps))) {
                    readProperties.put(drawProperty.highlightReader, drawGridObjects);
                    if (!inInterface) {
                        result.panelProperties.add(drawProperty.highlightReader);
                    }
                }
            } else if (previous!=null) // говорим клиенту что свойство надо удалить
                result.dropProperties.add(drawProperty);
        }

        for (GroupObjectInstance group : groups) // читаем highlight'ы
            if (group.propertyHighlight != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyHighlight, gridGroups, changedProps))
                    readProperties.put(group, gridGroups);
            }

        return readProperties;
    }

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectInstance> getPropertyGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    // возвращает какие объекты на форме не фиксируются
    private Set<GroupObjectInstance> getClassGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView == GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    // считывает все данные с формы
    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Set<GroupObjectInstance> classGroups) throws SQLException {

        applyFilters();
        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(GroupObjectInstance.getObjects(classGroups));
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectInstance group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.mapKeys, this));

                // закинем Order'ы
                for (Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, this));
                    queryOrders.put(order.getKey(), order.getValue());
                }

                for (ObjectInstance object : group.objects) {
                    query.properties.put(object, object.getExpr(query.mapKeys, this));
                    queryOrders.put(object, false);
                }
            }
        }

        FormData result = new FormData();

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.properties.put(property, property.propertyObject.getExpr(query.mapKeys, this));

        OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> resultSelect = query.execute(session.sql, queryOrders, 0, session.env);
        for (Entry<Map<ObjectInstance, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectInstance, Object> groupValue = new HashMap<ObjectInstance, Object>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        groupValue.put(object, row.getKey().get(object));
                    else
                        groupValue.put(object, object.getObjectValue().getValue());

            Map<PropertyDrawInstance, Object> propertyValues = new HashMap<PropertyDrawInstance, Object>();
            for (PropertyDrawInstance property : propertyDraws)
                propertyValues.put(property, row.getValue().get(property));

            result.add(groupValue, propertyValues);
        }

        return result;
    }

    public <P extends PropertyInterface> Set<FilterEntity> getEditFixedFilters(ClassFormEntity<T> editForm, PropertyObjectInstance<P> changeProperty, GroupObjectInstance selectionGroupObject) {
        Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();

        PropertyValueImplement<P> implement = changeProperty.getValueImplement();

        for (MaxChangeProperty<?, P> constrainedProperty : implement.property.getMaxChangeProperties(BL.getCheckConstrainedProperties())) {
            fixedFilters.add(
                    new NotFilterEntity(
                            new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                                    constrainedProperty.getPropertyObjectEntity(implement.mapping, editForm.getObject())
                            )
                    )
            );
        }

        ObjectEntity object = editForm.getObject();
        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        fixedFilters.add(filterEntity.getRemappedFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
                fixedFilters.addAll(filter.getResolveChangeFilters(editForm, implement));
            }
        }
        return fixedFilters;
    }

    public DialogInstance<T> createClassPropertyDialog(int viewID, int value) throws RemoteException, SQLException {
        ClassFormEntity<T> classForm = getPropertyDraw(viewID).propertyObject.getDialogClass().getDialogForm(BL.LM);
        return new DialogInstance<T>(classForm, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.getObject(), value, instanceFactory.computer);
    }

    public Object read(PropertyObjectInstance<?> property) throws SQLException {
        return property.read(session, this);
    }

    public DialogInstance<T> createObjectEditorDialog(int viewID) throws RemoteException, SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(BL);

        CustomClass objectClass = changeProperty.getDialogClass();
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

        Object currentObject = read(changeProperty);
        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }

        return currentObject == null
               ? null
               : new DialogInstance<T>(classForm, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.getObject(), currentObject, instanceFactory.computer);
    }

    public DialogInstance<T> createEditorPropertyDialog(int viewID) throws SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);

        Result<Property> aggProp = new Result<Property>();
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(aggProp, BL);

        ClassFormEntity<T> formEntity = changeProperty.getDialogClass().getDialogForm(BL.LM);
        Set<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, changeProperty, propertyDraw.toDraw);

        ObjectEntity dialogObject = formEntity.getObject();
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, read(changeProperty), instanceFactory.computer, additionalFilters);

        Property<PropertyInterface> filterProperty = aggProp.result;
        if (filterProperty != null) {
            PropertyDrawEntity filterPropertyDraw = formEntity.getPropertyDraw(filterProperty, dialogObject);
            if (filterPropertyDraw == null)
                filterPropertyDraw = formEntity.addPropertyDraw(filterProperty,
                        Collections.singletonMap(BaseUtils.single(filterProperty.interfaces), (PropertyObjectInterfaceEntity) dialogObject));
            dialog.initFilterPropertyDraw = filterPropertyDraw;
        }

        dialog.readOnly = changeProperty.getDialogClass().dialogReadOnly;
        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    // ---------------------------------------- Events ----------------------------------------

    private class AutoActionsRunner {
        private final RemoteForm form;
        private Iterator<PropertyObjectEntity> autoActionsIt;
        private Iterator<ClientAction> actionsIt;

        public AutoActionsRunner(RemoteForm form, List<PropertyObjectEntity> autoActions) {
            this.form = form;
            autoActionsIt = autoActions.iterator();
            actionsIt = new EmptyIterator<ClientAction>();
        }

        private void prepareNext() throws SQLException {
            while (autoActionsIt.hasNext() && !actionsIt.hasNext()) {
                PropertyObjectEntity autoAction = autoActionsIt.next();
                PropertyObjectInstance action = instanceFactory.getInstance(autoAction);
                if (action.isInInterface(null)) {
                    List<ClientAction> change
                            = changeProperty(action,
                                             read(action) == null ? true : null,
                                             form, null);
                    actionsIt = change.iterator();
                }
            }
        }

        private boolean hasNext() throws SQLException {
            prepareNext();
            return actionsIt.hasNext();
        }

        private ClientAction next() throws SQLException {
            if (hasNext()) {
                return actionsIt.next();
            }
            return null;
        }

        public List<ClientAction> run() throws SQLException {
            List<ClientAction> actions = new ArrayList<ClientAction>();
            while (hasNext()) {
                ClientAction action = next();
                actions.add(action);
                if (action instanceof ContinueAutoActionsClientAction || action instanceof StopAutoActionsClientAction) {
                    break;
                }
            }

            return actions;
        }
    }

    private AutoActionsRunner autoActionsRunner;
    public List<ClientAction> continueAutoActions() throws SQLException {
        if (autoActionsRunner != null) {
            return autoActionsRunner.run();
        }

        return new ArrayList<ClientAction>();
    }

    private List<ClientAction> fireObjectChanged(ObjectInstance object, RemoteForm form) throws SQLException {
        return fireEvent(form, object.entity);
    }

    public List<ClientAction> fireOnApply(RemoteForm form) throws SQLException {
        return fireEvent(form, FormEventType.APPLY);
    }

    public List<ClientAction> fireOnOk(RemoteForm form) throws SQLException {
        return fireEvent(form, FormEventType.OK);
    }

    public List<ClientAction> fireEvent(RemoteForm form, Object eventObject) throws SQLException {
        List<ClientAction> clientActions;
        List<PropertyObjectEntity> actionsOnEvent = entity.getActionsOnEvent(eventObject);
        if (actionsOnEvent != null) {
            autoActionsRunner = new AutoActionsRunner(form, actionsOnEvent);
            clientActions = autoActionsRunner.run();
        } else
            clientActions = new ArrayList<ClientAction>();

        for (FormEventListener listener : eventListeners)
            listener.handleEvent(eventObject);

        return clientActions;
    }

    private final WeakLinkedHashSet<FormEventListener> eventListeners = new WeakLinkedHashSet<FormEventListener>();
    public void addEventListener(FormEventListener listener) {
        eventListeners.add(listener);
    }

    public <P extends PropertyInterface> void fireChange(Property<P> property, PropertyChange<P> change) throws SQLException {
        entity.onChange(property, change, session, this);
    }
}