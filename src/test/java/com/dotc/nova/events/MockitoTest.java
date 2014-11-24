package com.dotc.nova.events;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class MockitoTest {
	@Test
	public void verifyMockitoCaptorWorksForMultipleParameterEmitting() {
		EventEmitter emitter = mock(EventEmitter.class);
		emitter.emit("event", "1", "2", "3", "4");
		ArgumentCaptor<String> paramCaptor = ArgumentCaptor.forClass(String.class);
		verify(emitter).emit(Mockito.eq("event"), paramCaptor.capture(), Mockito.anyString(), Mockito.same("3"),
				Mockito.eq("4"));
		String s = paramCaptor.getValue();
		Assert.assertEquals("1", s);
	}
}
