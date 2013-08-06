package com.dotc.nova;


public class EventDispatchConfig {
	public static enum WaitStrategy {
		MIN_LATENCY, MIN_CPU_USAGE, LOW_CPU_DEFAULT_LATENCY, LOW_LATENCY_DEFAULT_CPU
	};

	public final int eventBufferSize;
	public final WaitStrategy waitStrategy;
	public final boolean multipleProducers;

	private EventDispatchConfig(int eventBufferSize, WaitStrategy waitStrategy, boolean multipleProducers) {
		this.eventBufferSize = eventBufferSize;
		this.waitStrategy = waitStrategy;
		this.multipleProducers = multipleProducers;
	}

	public static class Builder {

		private WaitStrategy waitStrategy = WaitStrategy.MIN_CPU_USAGE;
		private int eventBufferSize = 10000;
		private boolean multipleProducers = true;

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

		public EventDispatchConfig build() {
			return new EventDispatchConfig(eventBufferSize, waitStrategy, multipleProducers);
		}
	}
}
