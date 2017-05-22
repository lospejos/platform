package lsfusion.server.data;

import lsfusion.base.ConcurrentIdentityWeakHashSet;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.WeakIdentityHashMap;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.base.col.MapFact;
import lsfusion.server.ServerLoggers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.netty.util.internal.ConcurrentIdentityHashMap;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.server.ServerLoggers.sqlLogger;

@Aspect
public class AssertSynchronizedAspect {

    private static Map<Object, WeakReference<Thread>> map = MapFact.getGlobalConcurrentIdentityWeakHashMap();

    private static Map<Thread, Boolean> suppress = MapFact.getGlobalConcurrentIdentityWeakHashMap();
    public static void pushSuppress() {
        suppress.put(Thread.currentThread(), true);
    }
    public static void popSuppress() {
        suppress.remove(Thread.currentThread());
    }

    @Around("execution(@lsfusion.server.data.AssertSynchronized * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        
        Thread currentThread = Thread.currentThread();
        WeakReference<Thread> prevWeakThread = map.put(object, new WeakReference<>(currentThread));
        if(prevWeakThread != null) { // работает не максимально надежно, но смысл в том что сам exception и так время от времени будет появляться
            Thread prevThread = prevWeakThread.get();
            if(prevThread != currentThread && !suppress.containsKey(currentThread) && !(prevThread != null && suppress.containsKey(prevThread)))
                ServerLoggers.assertLog(false, "ASSERT SYNCHRONIZED " + object + '\n' +
                     (prevThread == null? "DEAD" : prevThread.toString() + '\n' + ExceptionUtils.getStackTrace(prevThread.getStackTrace())) + " PREV CURRENT " + currentThread + '\n');
        }
        
        try {
            return thisJoinPoint.proceed();
        } finally {
            map.remove(object);
        }
    }

}
