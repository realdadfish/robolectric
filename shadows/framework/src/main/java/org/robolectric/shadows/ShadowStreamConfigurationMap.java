package org.robolectric.shadows;

import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build.VERSION_CODES;
import android.util.Size;
import java.util.Collection;
import java.util.Collections;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

/** Shadow for {@link StreamConfigurationMap} */
@Implements(value = StreamConfigurationMap.class, minSdk = VERSION_CODES.LOLLIPOP)
public class ShadowStreamConfigurationMap {

  private Collection<Size> outputSizes = Collections.emptyList();

  /** Convenience method which returns a new instance of {@link StreamConfigurationMap}. */
  public static StreamConfigurationMap newStreamConfigurationMap() {
    return ReflectionHelpers.callConstructor(StreamConfigurationMap.class);
  }

  @Implementation
  public <T> Size[] getOutputSizes(Class<T> klass) {
    return outputSizes.toArray(new Size[0]);
  }

  /** Sets the output sizes to be returned with future calls to {@link #getOutputSizes}. */
  public void setOutputSizes(Collection<Size> outputSizes) {
    this.outputSizes = outputSizes;
  }
}
