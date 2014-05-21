/**
 *  Copyright 2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.fabric8.test.jgroups.zookeeper;

import org.jgroups.JChannel;
import org.jgroups.util.Util;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PingTest extends TestBase {


    @Test
    public void testCluster() throws Exception {
        Util.waitUntilAllChannelsHaveSameSize(10000,1000,channels);

        // Tests unicasts from the first to the last
        JChannel first=channels[0], last=channels[NUM-1];
        for(int i=1; i <= 10; i++)
            first.send(last.getAddress(), i);

        List<Integer> msgs=receivers[NUM - 1].getList();
        Util.waitUntilListHasSize(msgs, 10, 10000, 1000);
        System.out.println("msgs = " + msgs);
        for(int i: Arrays.asList(1,2,3,4,5,6,7,8,9,10))
            assert msgs.contains(i);
        clearReceivers();

        // Tests multicasts
        for(int i=0; i < NUM; i++) {
            JChannel ch=channels[i];
            int num=(i+1)*10;
            for(int j=0; j <= 5; j++)
                ch.send(null, num+j);
        }

        final int expected_size=NUM * 6;
        final List<Integer> expected_numbers=new ArrayList<Integer>(expected_size);
        for(int i=0; i < NUM; i++) {
            int num=(i+1)*10;
            for(int j=0; j <= 5; j++)
                expected_numbers.add(num+j);
        }

        for(int i=0; i < NUM; i++) {
            List<Integer> list=receivers[i].getList();
            Util.waitUntilListHasSize(list, expected_size, 10000, 1000);
            System.out.println("list[" + i + "]: " + list);
        }

        for(int i=0; i < NUM; i++) {
            List<Integer> list=receivers[i].getList();
            for(int num: expected_numbers)
                assert list.contains(num);
        }

    }



}
