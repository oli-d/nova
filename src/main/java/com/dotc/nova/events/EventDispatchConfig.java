package com.dotc.nova.events;

public class EventDispatchConfig {
	public static enum WaitStrategy {
		MIN_LATENCY, MIN_CPU_USAGE, LOW_CPU_DEFAULT_LATENCY, LOW_LATENCY_DEFAULT_CPU
	};

	public static enum InsufficientCapacityStrategy {
		DROP_EVENTS, QUEUE_EVENTS, THROW_EXCEPTION, WAIT_UNTIL_SPACE_AVAILABLE
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
	public final DispatchThreadStrategy dispatchThreadStrategy;
	public final int numberOfConsumers;
	public final MultiConsumerDispatchStrategy multiConsumerDispatchStrategy;
	public final boolean warnOnUnhandledEvent;

	public EventDispatchConfig(Builder builder) {
		builder.validate();
		this.eventBufferSize = builder.eventBufferSize;
		this.waitStrategy = builder.waitStrategy;
		this.producerStrategy = builder.producerStrategy;
		this.insufficientCapacityStrategy = builder.insufficientCapacityStrategy;
		this.numberOfConsumers = builder.numberOfConsumers;
		this.dispatchThreadStrategy = builder.dispatchThreadStrategy;
		this.multiConsumerDispatchStrategy = builder.multiConsumerDispatchStrategy;
		this.warnOnUnhandledEvent = builder.warnOnUnhandledEvent;
	}

	// @Configurable
	public static class Builder {
		private WaitStrategy waitStrategy = WaitStrategy.MIN_CPU_USAGE;
		private int eventBufferSize = 1000;
		private ProducerStrategy producerStrategy = ProducerStrategy.MULTIPLE;
		private InsufficientCapacityStrategy insufficientCapacityStrategy = InsufficientCapacityStrategy.THROW_EXCEPTION;
		private DispatchThreadStrategy dispatchThreadStrategy = DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD;
		private int numberOfConsumers = 1;
		private MultiConsumerDispatchStrategy multiConsumerDispatchStrategy = MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ONE_CONSUMER;
		private boolean warnOnUnhandledEvent;

		public Builder() {
		}

		public Builder setEventBufferSize(int eventBufferSize) {
			this.eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventBufferSize);
			return this;
		}

		public Builder setProducerStrategy(ProducerStrategy producerStrategy) {
			this.producerStrategy = producerStrategy;
			return this;
		}

		public Builder setWaitStrategy(WaitStrategy waitStrategy) {
			this.waitStrategy = waitStrategy;
			return this;
		}

		public Builder setInsufficientCapacityStrategy(InsufficientCapacityStrategy queueFullStrategy) {
			this.insufficientCapacityStrategy = queueFullStrategy;
			return this;
		}

		public Builder setNumberOfConsumers(int numberOfConsumers) {
			this.numberOfConsumers = numberOfConsumers;
			if (numberOfConsumers <= 0) {
				this.numberOfConsumers = 1;
			}
			return this;
		}

		public Builder setMultiConsumerDispatchStrategy(MultiConsumerDispatchStrategy multiConsumerDispatchStrategy) {
			this.multiConsumerDispatchStrategy = multiConsumerDispatchStrategy;
			return this;
		}

		public Builder setDispatchThreadStrategy(DispatchThreadStrategy dispatchThreadStrategy) {
			this.dispatchThreadStrategy = dispatchThreadStrategy;
			return this;
		}

		public Builder setWarnOnUnhandledEvent(boolean warnOnUnhandledEvent) {
			this.warnOnUnhandledEvent = warnOnUnhandledEvent;
			return this;
		}

		private void validate() {
			if (numberOfConsumers > 1 && dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
				throw new IllegalArgumentException("DISPATCH_IN_EMITTER_THREAD strategy cannot be used with multiple consumers");
			}
		}

		public EventDispatchConfig build() {
			validate();
			return new EventDispatchConfig(this);
		}
	}
}
