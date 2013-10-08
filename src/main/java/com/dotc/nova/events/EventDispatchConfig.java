package com.dotc.nova.events;

public class EventDispatchConfig {
	public static enum WaitStrategy {
		MIN_LATENCY, MIN_CPU_USAGE, LOW_CPU_DEFAULT_LATENCY, LOW_LATENCY_DEFAULT_CPU
	};

	public static enum InsufficientCapacityStrategy {
		DROP_EVENTS, QUEUE_EVENTS, THROW_EXCEPTION
	};

	public final int eventBufferSize;
	public final WaitStrategy waitStrategy;
	public final InsufficientCapacityStrategy queueFullStrategy;
	public final boolean multipleProducers;
	public final boolean allowBatchProcessing;

	private EventDispatchConfig(int eventBufferSize, WaitStrategy waitStrategy, boolean multipleProducers, InsufficientCapacityStrategy queueFullStrategy, boolean allowBatchProcessing) {
		this.eventBufferSize = eventBufferSize;
		this.waitStrategy = waitStrategy;
		this.multipleProducers = multipleProducers;
		this.queueFullStrategy = queueFullStrategy;
		this.allowBatchProcessing = allowBatchProcessing;
	}

	public static class Builder {

		private WaitStrategy waitStrategy = WaitStrategy.MIN_CPU_USAGE;
		private int eventBufferSize = 10000;
		private boolean multipleProducers = true;
		private InsufficientCapacityStrategy queueFullStrategy = InsufficientCapacityStrategy.THROW_EXCEPTION;
		private boolean allowBatchProcessing = false;

		public Builder withEventBufferSize(int eventBufferSize) {
			this.eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventBufferSize);
			return this;
		}

		public Builder withMultipleProducers(boolean multipleProducers) {
			this.multipleProducers = multipleProducers;
			return this;
		}

		public Builder withWaitStrategy(WaitStrategy waitStrategy) {
			this.waitStrategy = waitStrategy;
			return this;
		}

		public Builder withQueueFullStrategy(InsufficientCapacityStrategy queueFullStrategy) {
			this.queueFullStrategy = queueFullStrategy;
			return this;
		}

		public Builder withBatchProcessing(boolean allowBatchProcessing) {
			this.allowBatchProcessing = allowBatchProcessing;
			return this;
		}

		public EventDispatchConfig build() {
			return new EventDispatchConfig(eventBufferSize, waitStrategy, multipleProducers, queueFullStrategy, allowBatchProcessing);
		}
	}
}
