package org.openmrs.eip.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.reflect.Whitebox;

public class CustomFileOffsetBackingStoreTest {
	
	@Spy
	private CustomOffsetBackingStore store;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Whitebox.setInternalState(CustomOffsetBackingStore.class, "paused", false);
		Whitebox.setInternalState(CustomOffsetBackingStore.class, "disabled", false);
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsPaused() {
		Whitebox.setInternalState(CustomOffsetBackingStore.class, "paused", true);
		
		assertThat(store.save((Void) -> true)).isFalse();
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsDisabled() {
		Whitebox.setInternalState(CustomOffsetBackingStore.class, "paused", true);
		
		assertThat(store.save((Void) -> true)).isFalse();
	}
	
	@Test
	public void save_shouldSaveOffsetsIfTheStoreIsNotPausedAndIsNotDisabled() {
		assertThat(store.save((Void) -> true)).isTrue();
	}
	
}
