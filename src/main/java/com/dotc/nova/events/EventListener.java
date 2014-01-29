package com.dotc.nova.events;

public interface EventListener<DataType> {
	public void handle(@SuppressWarnings("unchecked") DataType... data);
}
