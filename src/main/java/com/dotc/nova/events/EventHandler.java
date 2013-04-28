package com.dotc.nova.events;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;

public abstract class EventHandler<DataType> {
	public abstract void handle(DataType... data);

	public void handleEventWithData(Object... data) {
		if (data == null) {
			handle((DataType[]) null);
		}
		Class clazz;
		Object genericSuperclass = getClass().getGenericSuperclass();
		if (!(genericSuperclass instanceof ParameterizedType)) {
			clazz = Object.class;
		} else {
			clazz = (Class) (((ParameterizedType) genericSuperclass).getActualTypeArguments()[0]);
		}

		DataType[] dataArray = (DataType[]) Array.newInstance(clazz, data.length);
		for (int i = 0; i < data.length; i++) {
			dataArray[i] = (DataType) data[i];
		}
		handle(dataArray);
	}
}
