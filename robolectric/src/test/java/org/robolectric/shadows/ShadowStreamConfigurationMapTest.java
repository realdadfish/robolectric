package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.hardware.camera2.CameraCharacteristics.Key;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build.VERSION_CODES;
import android.util.Size;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Tests for {@link ShadowStreamConfigurationMap}. */
@Config(minSdk = VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class ShadowStreamConfigurationMapTest {

  private final Key key0 = new Key("key0", Integer.class);
  private final StreamConfigurationMap streamConfigurationMap =
      ShadowStreamConfigurationMap.newStreamConfigurationMap();

  @Test
  public void testGetOutputSizest() {
    Collection<Size> expectedOutputSizes = Arrays.asList(new Size(1920, 1080), new Size(1280, 720));
    shadowOf(streamConfigurationMap).setOutputSizes(expectedOutputSizes);

    assertThat(Arrays.asList(streamConfigurationMap.getOutputSizes(MediaRecorder.class)))
        .containsExactlyElementsIn(expectedOutputSizes);
  }

  @Test
  public void testGetOutputSizes_notSet() {
    assertThat(Arrays.asList(streamConfigurationMap.getOutputSizes(MediaRecorder.class))).isEmpty();
  }
}
