package com.dotc.nova.events;

import java.beans.ConstructorProperties;

public class CopyOfEventDispatchConfig {
	public static enum WaitStrategy {
		MIN_LATENCY, MIN_CPU_USAGE, LOW_CPU_DEFAULT_LATENCY, LOW_LATENCY_DEFAULT_CPU
	};

	public static enum InsufficientCapacityStrategy {
		DROP_EVENTS, QUEUE_EVENTS, THROW_EXCEPTION, WAIT_UNTIL_SPACE_AVAILABLE
	};

	public static enum BatchProcessingStrategy {
		PROCESS_ALL, DROP_OUTDATED
	};

	public static enum ProducerStrategy {
		SINGLE, MULTIPLE
	};

	public static enum DispatchThreadStrategy {
		DISPATCH_IN_EMITTER_THREAD, DISPATCH_IN_SPECIFIC_THREAD
	};

	public static enum MultiConsumerDispatchStrategy {
		DISPATCH_EVENTS_TO_ALL_CONSUMERS, DISPATCH_EVENTS_TO_ONE_CONSUMER
	};

	public final int eventBufferSize;
	public final WaitStrategy waitStrategy;
	public final InsufficientCapacityStrategy insufficientCapacityStrategy;
	public final ProducerStrategy producerStrategy;
	public final BatchProcessingStrategy batchProcessingStrategy;
	public final DispatchThreadStrategy dispatchThreadStrategy;
	public final int numberOfConsumers;
	public final MultiConsumerDispatchStrategy multiConsumerDispatchStrategy;
	public final boolean warnOnUnhandledEvent;

	public CopyOfEventDispatchConfig(Builder builder) {
		this.eventBufferSize = builder.eventBufferSize;
		this.waitStrategy = builder.waitStrategy;
		this.producerStrategy = builder.producerStrategy;
		this.insufficientCapacityStrategy = builder.insufficientCapacityStrategy;
		this.batchProcessingStrategy = builder.batchProcessingStrategy;
		this.numberOfConsumers = builder.numberOfConsumers;
		this.dispatchThreadStrategy = builder.dispatchThreadStrategy;
		this.multiConsumerDispatchStrategy = builder.multiConsumerDispatchStrategy;
		this.warnOnUnhandledEvent = builder.warnOnUnhandledEvent;
	}

	public static class Builder {
		private WaitStrategy waitStrategy = WaitStrategy.MIN_CPU_USAGE;
		private int eventBufferSize = 1000;
		private ProducerStrategy producerStrategy = ProducerStrategy.MULTIPLE;
		private InsufficientCapacityStrategy insufficientCapacityStrategy = InsufficientCapacityStrategy.THROW_EXCEPTION;
		private BatchProcessingStrategy batchProcessingStrategy = BatchProcessingStrategy.PROCESS_ALL;
		private DispatchThreadStrategy dispatchThreadStrategy = DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD;
		private int numberOfConsumers = 1;
		private MultiConsumerDispatchStrategy multiConsumerDispatchStrategy = MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ONE_CONSUMER;
		private boolean warnOnUnhandledEvent;

		@ConstructorProperties({ "eventBufferSize", "waitStrategy", "producerStrategy", "dispatchThreadStrategy", "insufficientCapacityStrategy", "numberOfConsumers", "multiConsumerDispatchStrategy",
				"batchProcessingStrategy", "warnOnUnhandledEvent" })
		public Builder(int eventBufferSize, WaitStrategy waitStrategy, ProducerStrategy producerStrategy, DispatchThreadStrategy dispatchThreadStrategy,
				InsufficientCapacityStrategy insufficientCapacityStrategy, int numberOfConsumers, MultiConsumerDispatchStrategy multiConsumerDispatchStrategy,
				BatchProcessingStrategy batchProcessingStrategy, boolean warnOnUnhandledEvent) {
			this.eventBufferSize = eventBufferSize;
			this.waitStrategy = waitStrategy;
			this.producerStrategy = producerStrategy;
			this.dispatchThreadStrategy = dispatchThreadStrategy;
			this.insufficientCapacityStrategy = insufficientCapacityStrategy;
			this.numberOfConsumers = numberOfConsumers;
			this.multiConsumerDispatchStrategy = multiConsumerDispatchStrategy;
			this.batchProcessingStrategy = batchProcessingStrategy;
			this.warnOnUnhandledEvent = warnOnUnhandledEvent;
			validate();
		}

		public Builder() {
		}

		public Builder withEventBufferSize(int eventBufferSize) {
			this.eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventBufferSize);
			return this;
		}

		public Builder withProducerStrategy(ProducerStrategy producerStrategy) {
			this.producerStrategy = producerStrategy;
			return this;
		}

		public Builder withWaitStrategy(WaitStrategy waitStrategy) {
			this.waitStrategy = waitStrategy;
			return this;
		}

		public Builder withInsufficientCapacityStrategy(InsufficientCapacityStrategy queueFullStrategy) {
			this.insufficientCapacityStrategy = queueFullStrategy;
			return this;
		}

		public Builder withBatchProcessingStrategy(BatchProcessingStrategy batchProcessingStrategy) {
			this.batchProcessingStrategy = batchProcessingStrategy;
			return this;
		}

		public Builder withNumberOfConsumers(int numberOfConsumers) {
			this.numberOfConsumers = numberOfConsumers;
			return this;
		}

		public Builder withMultiConsumerDispatchStrategy(MultiConsumerDispatchStrategy multiConsumerDispatchStrategy) {
			this.multiConsumerDispatchStrategy = multiConsumerDispatchStrategy;
			return this;
		}

		public Builder withDispatchThreadStrategy(DispatchThreadStrategy dispatchThreadStrategy) {
			this.dispatchThreadStrategy = dispatchThreadStrategy;
			return this;
		}

		public Builder withWarnOnUnhandledEvent(boolean warnOnUnhandledEvent) {
			this.warnOnUnhandledEvent = warnOnUnhandledEvent;
			return this;
		}

		public void validate() {
			if (numberOfConsumers <= 0) {
				numberOfConsumers = 1;
			}
			if (numberOfConsumers > 1 && dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
				throw new IllegalArgumentException("DISPATCH_IN_EMITTER_THREAD strategy cannot be used with multiple consumers");
			}
			if (numberOfConsumers > 1 && batchProcessingStrategy != BatchProcessingStrategy.PROCESS_ALL) {
				throw new IllegalArgumentException("specific batch processing strategy cannot be used with multiple consumers");
			}
		}

		public CopyOfEventDispatchConfig build() {
			validate();
			return new CopyOfEventDispatchConfig(this);
		}
	}
}
