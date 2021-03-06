/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.core.set;

import io.atomix.core.PrimitiveTypes;
import io.atomix.primitive.Consistency;
import io.atomix.primitive.DistributedPrimitiveBuilder;
import io.atomix.primitive.Persistence;
import io.atomix.primitive.Replication;

/**
 * Builder for distributed set.
 *
 * @param <E> type set elements.
 */
public abstract class DistributedSetBuilder<E> extends DistributedPrimitiveBuilder<DistributedSetBuilder<E>, DistributedSet<E>> {
  public DistributedSetBuilder(String name) {
    super(PrimitiveTypes.set(), name);
  }

  @Override
  protected Consistency defaultConsistency() {
    return Consistency.SEQUENTIAL;
  }

  @Override
  protected Persistence defaultPersistence() {
    return Persistence.EPHEMERAL;
  }

  @Override
  protected Replication defaultReplication() {
    return Replication.SYNCHRONOUS;
  }
}
