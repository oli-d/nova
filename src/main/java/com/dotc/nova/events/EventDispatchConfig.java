package com.dotc.nova.events;

public class EventDispatchConfig {
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

	public final int eventBufferSize;
	public final WaitStrategy waitStrategy;
	public final InsufficientCapacityStrategy insufficientCapacityStrategy;
	public final ProducerStrategy producerStrategy;
	public final BatchProcessingStrategy batchProcessingStrategy;
	public final DispatchThreadStrategy dispatchThreadStrategy;
	public final int numberOfDispatchThreads;
	public final boolean warnOnUnhandledEvent;

	private EventDispatchConfig(int eventBufferSize, WaitStrategy waitStrategy, ProducerStrategy producerStrategy, InsufficientCapacityStrategy queueFullStrategy,
			BatchProcessingStrategy batchProcessingStrategy, DispatchThreadStrategy dispatchThreadStrategy, int numberOfDispatchThreads, boolean warnOnUnhandledEvent) {
		this.eventBufferSize = eventBufferSize;
		this.waitStrategy = waitStrategy;
		this.producerStrategy = producerStrategy;
		this.insufficientCapacityStrategy = queueFullStrategy;
		this.batchProcessingStrategy = batchProcessingStrategy;
		this.numberOfDispatchThreads = numberOfDispatchThreads;
		this.dispatchThreadStrategy = dispatchThreadStrategy;
		this.warnOnUnhandledEvent = warnOnUnhandledEvent;
	}

	public static class Builder {

		private WaitStrategy waitStrategy = WaitStrategy.MIN_CPU_USAGE;
		private int eventBufferSize = 10000;
		private ProducerStrategy producerStrategy = ProducerStrategy.MULTIPLE;
		private InsufficientCapacityStrategy queueFullStrategy = InsufficientCapacityStrategy.THROW_EXCEPTION;
		private BatchProcessingStrategy batchProcessingStrategy = BatchProcessingStrategy.PROCESS_ALL;
		private int numberOfDispatchThreads = 1;
		private DispatchThreadStrategy dispatchThreadStrategy;
		private boolean warnOnUnhandledEvent;

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
			this.queueFullStrategy = queueFullStrategy;
			return this;
		}

		public Builder withBatchProcessingStrategy(BatchProcessingStrategy batchProcessingStrategy) {
			this.batchProcessingStrategy = batchProcessingStrategy;
			return this;
		}

		public Builder withNumberOfDispatchThreads(int numberOfDispatchThreads) {
			this.numberOfDispatchThreads = numberOfDispatchThreads;
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

		public EventDispatchConfig build() {
			if (numberOfDispatchThreads < 0) {
				numberOfDispatchThreads = 1;
			}
			if (numberOfDispatchThreads > 1 && dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
				throw new IllegalArgumentException("DISPATCH_IN_EMITTER_THREAD strategy cannot be used with multiple dispatch threads");
			}
			if (numberOfDispatchThreads > 1 && batchProcessingStrategy != BatchProcessingStrategy.PROCESS_ALL) {
				throw new IllegalArgumentException("specific batch processing strategy cannot be used with multiple dispatch threads");
			}
			return new EventDispatchConfig(eventBufferSize, waitStrategy, producerStrategy, queueFullStrategy, batchProcessingStrategy, dispatchThreadStrategy, numberOfDispatchThreads,
					warnOnUnhandledEvent);
		}
	}
}