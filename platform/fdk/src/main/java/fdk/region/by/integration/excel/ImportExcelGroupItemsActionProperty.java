package fdk.region.by.integration.excel;

import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import fdk.integration.Item;
import fdk.integration.ItemGroup;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelGroupItemsActionProperty extends ImportExcelActionProperty {

    public ImportExcelGroupItemsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.getDefinedInstance(false, false, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {

                    ImportData importData = new ImportData();

                    importData.setParentGroupsList(importGroupItems(file, true));
                    importData.setItemGroupsList(importGroupItems(file, false));

                    new ImportActionProperty(LM, importData, context).makeImport();

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BiffException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ItemGroup> importGroupItems(byte[] file, Boolean parents) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<ItemGroup> data = new ArrayList<ItemGroup>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String groupID = parseString(sheet.getCell(0, i).getContents());
            String name = parseString(sheet.getCell(1, i).getContents());
            String parentID = parseString(sheet.getCell(2, i).getContents());
            if (parents)
                data.add(new ItemGroup(groupID, null, parentID));
            else
                data.add(new ItemGroup(groupID, name, null));
        }

        return data;
    }
}