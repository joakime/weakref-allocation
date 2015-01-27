/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.ref;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.ObjectName;

/**
 * Weak reference objects, which do not prevent their referents from being
 * made finalizable, finalized, and then reclaimed.  Weak references are most
 * often used to implement canonicalizing mappings.
 *
 * <p> Suppose that the garbage collector determines at a certain point in time
 * that an object is <a href="package-summary.html#reachability">weakly
 * reachable</a>.  At that time it will atomically clear all weak references to
 * that object and all weak references to any other weakly-reachable objects
 * from which that object is reachable through a chain of strong and soft
 * references.  At the same time it will declare all of the formerly
 * weakly-reachable objects to be finalizable.  At the same time or at some
 * later time it will enqueue those newly-cleared weak references that are
 * registered with reference queues.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */
public class WeakReference<T> extends Reference<T> {

    private static volatile Managed mbean;

    static
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(1000);
                    Managed mbean = new Managed();
                    WeakReference.mbean = mbean;
                    ObjectName objectName = ObjectName.getInstance("java.lang.ref:type=WeakReference");
                    ManagementFactory.getPlatformMBeanServer().registerMBean(mbean, objectName);
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                    throw new Error(x);
                }
            }
        }).start();

    }

    /**
     * Creates a new weak reference that refers to the given object.  The new
     * reference is not registered with any queue.
     *
     * @param referent object the new weak reference will refer to
     */
    public WeakReference(T referent) {
        this(referent, null);
    }

    /**
     * Creates a new weak reference that refers to the given object and is
     * registered with the given queue.
     *
     * @param referent object the new weak reference will refer to
     * @param queue the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     */
    public WeakReference(T referent, ReferenceQueue<? super T> queue) {
        super(referent, queue);
        Managed mbean = WeakReference.mbean;
        if (mbean != null)
            mbean.record(referent);
    }

    public interface ManagedMBean
    {
        public boolean isEnabled();
        
        public void setEnabled(boolean flag);
        
        public boolean toggleEnabled();
        
        public int getStackdumpInterval();
        
        public void setStackdumpInterval(int interval);
        
        public void reset();

        public String dumpByName();
        
        public String dumpByCount();
    }

    public static class Managed implements ManagedMBean
    {
        private Map<String, Long> weaks = new HashMap<>();
        private boolean enabled = true;
        private int stackdumpInterval = 100;

        public void record(Object referent)
        {
            if (!enabled)
            {
                return;
            }

            synchronized (this)
            {
                String key = referent == null ? "null" : referent.getClass().getName();
                Long count = weaks.get(key);
                Long value = count == null ? 1L : count + 1;
                weaks.put(key,value);
                if (value % stackdumpInterval == 0)
                    Thread.dumpStack();
            }
        }
        
        @Override
        public int getStackdumpInterval()
        {
            return stackdumpInterval;
        }
        
        @Override
        public boolean isEnabled()
        {
            return enabled;
        }
        
        @Override
        public void setEnabled(boolean flag)
        {
            this.enabled = flag;
        }
        
        @Override
        public void setStackdumpInterval(int interval)
        {
            if (interval <= 0)
            {
                this.stackdumpInterval = 1;
            }
            else
            {
                this.stackdumpInterval = interval;
            }
        }
        
        @Override
        public boolean toggleEnabled()
        {
            enabled = !enabled;
            return enabled;
        }

        @Override
        public void reset()
        {
            synchronized (this)
            {
                weaks.clear();
            }
        }

        @Override
        public String dumpByName()
        {
            List<Map.Entry<String, Long>> entries = new ArrayList<>();
            synchronized (this)
            {
                entries.addAll(weaks.entrySet());
            }
            Collections.sort(entries,ByNameComparator.INSTANCE);
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Long> entry : entries)
                result.append(entry.getKey())
                      .append(" -> ")
                      .append(entry.getValue())
                      .append(System.lineSeparator());
            return result.toString();
        }
        
        @Override
        public String dumpByCount()
        {
            List<Map.Entry<String, Long>> entries = new ArrayList<>();
            synchronized (this)
            {
                entries.addAll(weaks.entrySet());
            }
            Collections.sort(entries,ByCountComparator.INSTANCE);
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Long> entry : entries)
                result.append(entry.getValue())
                      .append(" -> ")
                      .append(entry.getKey())
                      .append(System.lineSeparator());
            return result.toString();
        }
    }
    
    private static class ByNameComparator implements Comparator<Map.Entry<String,Long>>
    {
        public static final ByNameComparator INSTANCE = new ByNameComparator();

        @Override
        public int compare(Entry<String, Long> o1, Entry<String, Long> o2)
        {
            return o1.getKey().compareTo(o2.getKey());
        }
    }
    
    private static class ByCountComparator implements Comparator<Map.Entry<String,Long>>
    {
        public static final ByCountComparator INSTANCE = new ByCountComparator();

        @Override
        public int compare(Entry<String, Long> o1, Entry<String, Long> o2)
        {
            Long l1 = o1.getValue();
            Long l2 = o2.getValue();
            if ((l1 == null) || (l2 == null))
            {
                return -1;
            }
            int diff = l1.intValue() - l2.intValue();
            return diff;
        }
    }
}
