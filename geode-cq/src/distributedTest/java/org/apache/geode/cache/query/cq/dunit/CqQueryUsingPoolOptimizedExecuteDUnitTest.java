/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.cache.query.cq.dunit;

import org.junit.experimental.categories.Category;

import org.apache.geode.cache.query.internal.cq.CqServiceImpl;
import org.apache.geode.test.dunit.Invoke;
import org.apache.geode.test.dunit.SerializableRunnable;
import org.apache.geode.test.junit.categories.ClientSubscriptionTest;

/**
 * Test class for testing {@link CqServiceImpl#EXECUTE_QUERY_DURING_INIT} flag
 *
 */
@Category({ClientSubscriptionTest.class})
public class CqQueryUsingPoolOptimizedExecuteDUnitTest extends CqQueryUsingPoolDUnitTest {

  public CqQueryUsingPoolOptimizedExecuteDUnitTest() {
    super();
  }

  @Override
  public final void postSetUpCqQueryUsingPoolDUnitTest() throws Exception {
    Invoke.invokeInEveryVM(new SerializableRunnable("getSystem") {
      public void run() {
        CqServiceImpl.EXECUTE_QUERY_DURING_INIT = false;
      }
    });
  }

  @Override
  public final void preTearDownCacheTestCase() throws Exception {
    Invoke.invokeInEveryVM(new SerializableRunnable("getSystem") {
      public void run() {
        CqServiceImpl.EXECUTE_QUERY_DURING_INIT = true;
      }
    });
  }
}
