/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.copycat.atomic.state;

import io.atomix.catalogue.client.Command;
import io.atomix.catalogue.client.Operation;
import io.atomix.catalogue.client.Query;
import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializeWith;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.BuilderPool;

/**
 * Atomic reference commands.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class AtomicValueCommands {

  private AtomicValueCommands() {
  }

  /**
   * Abstract value command.
   */
  public static abstract class ValueCommand<V> implements Command<V>, CatalystSerializable {
    protected ConsistencyLevel consistency = ConsistencyLevel.LINEARIZABLE;
    protected long ttl;

    @Override
    public PersistenceLevel persistence() {
      return ttl > 0 ? PersistenceLevel.EPHEMERAL : PersistenceLevel.PERSISTENT;
    }

    /**
     * Returns the consistency level.
     *
     * @return The consistency level.
     */
    public ConsistencyLevel consistency() {
      return consistency;
    }

    /**
     * Returns the time to live in milliseconds.
     *
     * @return The time to live in milliseconds.
     */
    public long ttl() {
      return ttl;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      buffer.writeByte(consistency.ordinal())
        .writeLong(ttl);
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      consistency = ConsistencyLevel.values()[buffer.readByte()];
      ttl = buffer.readLong();
    }

    /**
     * Base reference command builder.
     */
    public static abstract class Builder<T extends Builder<T, U, V>, U extends ValueCommand<V>, V> extends Command.Builder<T, U, V> {
      protected Builder(BuilderPool<T, U> pool) {
        super(pool);
      }

      /**
       * Sets the consistency level.
       *
       * @param consistency The consistency level.
       * @return The command builder.
       */
      @SuppressWarnings("unchecked")
      public T withConsistency(ConsistencyLevel consistency) {
        if (consistency == null)
          throw new NullPointerException("consistency cannot be null");
        command.consistency = consistency;
        return (T) this;
      }

      /**
       * Sets the time to live.
       *
       * @param ttl The time to live in milliseconds..
       * @return The command builder.
       */
      @SuppressWarnings("unchecked")
      public T withTtl(long ttl) {
        command.ttl = ttl;
        return (T) this;
      }
    }
  }

  /**
   * Abstract value query.
   */
  public static abstract class ValueQuery<V> implements Query<V>, CatalystSerializable {
    protected ConsistencyLevel consistency = ConsistencyLevel.LINEARIZABLE;

    @Override
    public ConsistencyLevel consistency() {
      return consistency;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      buffer.writeByte(consistency.ordinal());
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      consistency = ConsistencyLevel.values()[buffer.readByte()];
    }

    /**
     * Base reference query builder.
     */
    public static abstract class Builder<T extends Builder<T, U, V>, U extends ValueQuery<V>, V> extends Query.Builder<T, U, V> {
      protected Builder(BuilderPool<T, U> pool) {
        super(pool);
      }

      /**
       * Sets the query consistency level.
       *
       * @param consistency The query consistency level.
       * @return The query builder.
       */
      @SuppressWarnings("unchecked")
      public T withConsistency(ConsistencyLevel consistency) {
        if (consistency == null)
          throw new NullPointerException("consistency cannot be null");
        query.consistency = consistency;
        return (T) this;
      }
    }
  }

  /**
   * Get query.
   */
  @SerializeWith(id=460)
  public static class Get<T> extends ValueQuery<T> {

    /**
     * Returns a new get query builder.
     *
     * @return A new get query builder.
     */
    @SuppressWarnings("unchecked")
    public static <T> Builder<T> builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    /**
     * Get query builder.
     */
    public static class Builder<T> extends ValueQuery.Builder<Builder<T>, Get<T>, T> {
      public Builder(BuilderPool<Builder<T>, Get<T>> pool) {
        super(pool);
      }

      @Override
      protected Get<T> create() {
        return new Get<>();
      }
    }
  }

  /**
   * Set command.
   */
  @SerializeWith(id=461)
  public static class Set extends ValueCommand<Void> {

    /**
     * Returns a new set command builder.
     *
     * @return A new set command builder.
     */
    public static Builder builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    private Object value;

    /**
     * Returns the command value.
     *
     * @return The command value.
     */
    public Object value() {
      return value;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      serializer.writeObject(value, buffer);
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      value = serializer.readObject(buffer);
    }

    @Override
    public String toString() {
      return String.format("%s[value=%s]", getClass().getSimpleName(), value);
    }

    /**
     * Put command builder.
     */
    public static class Builder extends ValueCommand.Builder<Builder, Set, Void> {
      public Builder(BuilderPool<Builder, Set> pool) {
        super(pool);
      }

      @Override
      protected Set create() {
        return new Set();
      }

      /**
       * Sets the command value.
       *
       * @param value The command value.
       * @return The command builder.
       */
      public Builder withValue(Object value) {
        command.value = value;
        return this;
      }
    }
  }

  /**
   * Compare and set command.
   */
  @SerializeWith(id=462)
  public static class CompareAndSet extends ValueCommand<Boolean> {

    /**
     * Returns a new compare and set command builder.
     *
     * @return A new compare and set command builder.
     */
    public static Builder builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    private Object expect;
    private Object update;

    /**
     * Returns the expected value.
     *
     * @return The expected value.
     */
    public Object expect() {
      return expect;
    }

    /**
     * Returns the updated value.
     *
     * @return The updated value.
     */
    public Object update() {
      return update;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      serializer.writeObject(expect, buffer);
      serializer.writeObject(update, buffer);
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      expect = serializer.readObject(buffer);
      update = serializer.readObject(buffer);
    }

    @Override
    public String toString() {
      return String.format("%s[expect=%s, update=%s]", getClass().getSimpleName(), expect, update);
    }

    /**
     * Compare and set command builder.
     */
    public static class Builder extends ValueCommand.Builder<Builder, CompareAndSet, Boolean> {
      public Builder(BuilderPool<Builder, CompareAndSet> pool) {
        super(pool);
      }

      @Override
      protected CompareAndSet create() {
        return new CompareAndSet();
      }

      /**
       * Sets the expected value.
       *
       * @param expect The expected value.
       * @return The command builder.
       */
      public Builder withExpect(Object expect) {
        command.expect = expect;
        return this;
      }

      /**
       * Sets the updated value.
       *
       * @param update The updated value.
       * @return The command builder.
       */
      public Builder withUpdate(Object update) {
        command.update = update;
        return this;
      }
    }
  }

  /**
   * Get and set command.
   */
  @SerializeWith(id=463)
  public static class GetAndSet<T> extends ValueCommand<T> {

    /**
     * Returns a new get and set command builder.
     *
     * @return A new get and set command builder.
     */
    @SuppressWarnings("unchecked")
    public static <T> Builder<T> builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    private Object value;

    /**
     * Returns the command value.
     *
     * @return The command value.
     */
    public Object value() {
      return value;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      serializer.writeObject(value, buffer);
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      value = serializer.readObject(buffer);
    }

    @Override
    public String toString() {
      return String.format("%s[value=%s]", getClass().getSimpleName(), value);
    }

    /**
     * Put command builder.
     */
    public static class Builder<T> extends ValueCommand.Builder<Builder<T>, GetAndSet<T>, T> {
      public Builder(BuilderPool<Builder<T>, GetAndSet<T>> pool) {
        super(pool);
      }

      @Override
      protected GetAndSet<T> create() {
        return new GetAndSet<>();
      }

      /**
       * Sets the command value.
       *
       * @param value The command value.
       * @return The command builder.
       */
      public Builder<T> withValue(Object value) {
        command.value = value;
        return this;
      }
    }
  }

  /**
   * Change listen.
   */
  @SerializeWith(id=464)
  public static class Listen implements Command<Void>, CatalystSerializable {

    /**
     * Returns a new change listen builder.
     *
     * @return A new change listen builder.
     */
    public static Builder builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {

    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {

    }

    /**
     * Change listen builder.
     */
    public static class Builder extends Command.Builder<Builder, Listen, Void> {
      public Builder(BuilderPool<Builder, Listen> pool) {
        super(pool);
      }

      @Override
      protected Listen create() {
        return new Listen();
      }
    }
  }

  /**
   * Change unlisten.
   */
  @SerializeWith(id=465)
  public static class Unlisten implements Command<Void>, CatalystSerializable {

    /**
     * Returns a new change unlisten builder.
     *
     * @return A new change unlisten builder.
     */
    public static Builder builder() {
      return Operation.builder(Builder.class, Builder::new);
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {

    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {

    }

    /**
     * Change unlisten builder.
     */
    public static class Builder extends Command.Builder<Builder, Unlisten, Void> {
      public Builder(BuilderPool<Builder, Unlisten> pool) {
        super(pool);
      }

      @Override
      protected Unlisten create() {
        return new Unlisten();
      }
    }
  }

}
