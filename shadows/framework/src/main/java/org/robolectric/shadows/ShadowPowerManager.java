package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static com.google.common.base.Preconditions.checkState;
import static org.robolectric.shadows.ShadowApplication.getInstance;

import android.Manifest.permission;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.os.Binder;
import android.os.PowerManager;
import android.os.WorkSource;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.HiddenApi;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;

/** Shadow of PowerManager */
@Implements(value = PowerManager.class, looseSignatures = true)
public class ShadowPowerManager {
  private boolean isScreenOn = true;
  private boolean isInteractive = true;
  private boolean isPowerSaveMode = false;
  private boolean isDeviceIdleMode = false;
  private boolean isLightDeviceIdleMode = false;

  @PowerManager.LocationPowerSaveMode
  private int locationMode = PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF;

  private List<String> rebootReasons = new ArrayList<String>();
  private Map<String, Boolean> ignoringBatteryOptimizations = new HashMap<>();

  private int thermalStatus = 0;
  // Intentionally use Object instead of PowerManager.OnThermalStatusChangedListener to avoid
  // ClassLoader exceptions on earlier SDKs that don't have this class.
  private final Set<Object> thermalListeners = new HashSet<>();

  private final Set<String> ambientDisplaySuppressionTokens =
      Collections.synchronizedSet(new HashSet<>());
  private volatile boolean isAmbientDisplayAvailable = true;
  private volatile boolean isRebootingUserspaceSupported = false;

  @Implementation
  protected PowerManager.WakeLock newWakeLock(int flags, String tag) {
    PowerManager.WakeLock wl = Shadow.newInstanceOf(PowerManager.WakeLock.class);
    ((ShadowWakeLock) Shadow.extract(wl)).setTag(tag);
    getInstance().addWakeLock(wl);
    return wl;
  }

  @Implementation
  protected boolean isScreenOn() {
    return isScreenOn;
  }

  public void setIsScreenOn(boolean screenOn) {
    isScreenOn = screenOn;
  }

  @Implementation(minSdk = LOLLIPOP)
  protected boolean isInteractive() {
    return isInteractive;
  }

  public void setIsInteractive(boolean interactive) {
    isInteractive = interactive;
  }

  @Implementation(minSdk = LOLLIPOP)
  protected boolean isPowerSaveMode() {
    return isPowerSaveMode;
  }

  public void setIsPowerSaveMode(boolean powerSaveMode) {
    isPowerSaveMode = powerSaveMode;
  }

  private Map<Integer, Boolean> supportedWakeLockLevels = new HashMap<>();

  @Implementation(minSdk = LOLLIPOP)
  protected boolean isWakeLockLevelSupported(int level) {
    return supportedWakeLockLevels.containsKey(level) ? supportedWakeLockLevels.get(level) : false;
  }

  public void setIsWakeLockLevelSupported(int level, boolean supported) {
    supportedWakeLockLevels.put(level, supported);
  }

  /** @return false by default, or the value specified via {@link #setIsDeviceIdleMode(boolean)} */
  @Implementation(minSdk = M)
  protected boolean isDeviceIdleMode() {
    return isDeviceIdleMode;
  }

  /** Sets the value returned by {@link #isDeviceIdleMode()}. */
  public void setIsDeviceIdleMode(boolean isDeviceIdleMode) {
    this.isDeviceIdleMode = isDeviceIdleMode;
  }

  /**
   * @return false by default, or the value specified via {@link #setIsLightDeviceIdleMode(boolean)}
   */
  @Implementation(minSdk = N)
  protected boolean isLightDeviceIdleMode() {
    return isLightDeviceIdleMode;
  }

  /** Sets the value returned by {@link #isLightDeviceIdleMode()}. */
  public void setIsLightDeviceIdleMode(boolean lightDeviceIdleMode) {
    isLightDeviceIdleMode = lightDeviceIdleMode;
  }

  /**
   * Returns how location features should behave when battery saver is on. When battery saver is
   * off, this will always return {@link #LOCATION_MODE_NO_CHANGE}.
   */
  @Implementation(minSdk = P)
  @PowerManager.LocationPowerSaveMode
  protected int getLocationPowerSaveMode() {
    if (!isPowerSaveMode()) {
      return PowerManager.LOCATION_MODE_NO_CHANGE;
    }
    return locationMode;
  }

  /** Sets the value returned by {@link #getLocationPowerSaveMode()} when battery saver is on. */
  public void setLocationPowerSaveMode(@PowerManager.LocationPowerSaveMode int locationMode) {
    checkState(
        locationMode >= PowerManager.MIN_LOCATION_MODE,
        "Location Power Save Mode must be at least " + PowerManager.MIN_LOCATION_MODE);
    checkState(
        locationMode <= PowerManager.MAX_LOCATION_MODE,
        "Location Power Save Mode must be no more than " + PowerManager.MAX_LOCATION_MODE);
    this.locationMode = locationMode;
  }

  /** This function returns the current thermal status of the device. */
  @Implementation(minSdk = Q)
  protected int getCurrentThermalStatus() {
    return thermalStatus;
  }

  /** This function adds a listener for thermal status change. */
  @Implementation(minSdk = Q)
  protected void addThermalStatusListener(Object listener) {
    checkState(
        listener instanceof PowerManager.OnThermalStatusChangedListener,
        "Listener must implement PowerManager.OnThermalStatusChangedListener");
    this.thermalListeners.add(listener);
  }

  /** This function removes a listener for thermal status change. */
  @Implementation(minSdk = Q)
  protected void removeThermalStatusListener(Object listener) {
    checkState(
        listener instanceof PowerManager.OnThermalStatusChangedListener,
        "Listener must implement PowerManager.OnThermalStatusChangedListener");
    this.thermalListeners.remove(listener);
  }

  /** Sets the value returned by {@link #getCurrentThermalStatus()}. */
  public void setCurrentThermalStatus(int thermalStatus) {
    checkState(
        thermalStatus >= PowerManager.THERMAL_STATUS_NONE,
        "Thermal status must be at least " + PowerManager.THERMAL_STATUS_NONE);
    checkState(
        locationMode <= PowerManager.THERMAL_STATUS_SHUTDOWN,
        "Thermal status must be no more than " + PowerManager.THERMAL_STATUS_SHUTDOWN);
    this.thermalStatus = thermalStatus;
    for (Object listener : thermalListeners) {
      ((PowerManager.OnThermalStatusChangedListener) listener)
          .onThermalStatusChanged(thermalStatus);
    }
  }

  /** Discards the most recent {@code PowerManager.WakeLock}s */
  @Resetter
  public static void reset() {
    ShadowApplication shadowApplication = ShadowApplication.getInstance();
    if (shadowApplication != null) {
      shadowApplication.clearWakeLocks();
    }
  }

  /**
   * Retrieves the most recent wakelock registered by the application
   *
   * @return Most recent wake lock.
   */
  public static PowerManager.WakeLock getLatestWakeLock() {
    ShadowApplication shadowApplication = Shadow.extract(RuntimeEnvironment.application);
    return shadowApplication.getLatestWakeLock();
  }

  @Implementation(minSdk = M)
  protected boolean isIgnoringBatteryOptimizations(String packageName) {
    Boolean result = ignoringBatteryOptimizations.get(packageName);
    return result == null ? false : result;
  }

  public void setIgnoringBatteryOptimizations(String packageName, boolean value) {
    ignoringBatteryOptimizations.put(packageName, Boolean.valueOf(value));
  }

  @Implementation
  protected void reboot(String reason) {
    if (RuntimeEnvironment.getApiLevel() >= R
        && "userspace".equals(reason)
        && !isRebootingUserspaceSupported()) {
      throw new UnsupportedOperationException(
          "Attempted userspace reboot on a device that doesn't support it");
    }
    rebootReasons.add(reason);
  }

  /** Returns the number of times {@link #reboot(String)} was called. */
  public int getTimesRebooted() {
    return rebootReasons.size();
  }

  /** Returns the list of reasons for each reboot, in chronological order. */
  public ImmutableList<String> getRebootReasons() {
    return ImmutableList.copyOf(rebootReasons);
  }

  /** Sets the value returned by {@link #isAmbientDisplayAvailable()}. */
  public void setAmbientDisplayAvailable(boolean available) {
    this.isAmbientDisplayAvailable = available;
  }

  /** Sets the value returned by {@link #isRebootingUserspaceSupported()}. */
  public void setIsRebootingUserspaceSupported(boolean supported) {
    this.isRebootingUserspaceSupported = supported;
  }

  /**
   * Returns true by default, or the value specified via {@link
   * #setAmbientDisplayAvailable(boolean)}.
   */
  @Implementation(minSdk = R)
  @SystemApi
  @RequiresPermission(permission.READ_DREAM_STATE)
  protected boolean isAmbientDisplayAvailable() {
    return isAmbientDisplayAvailable;
  }

  /**
   * If true, suppress the device's ambient display. Ambient display is defined as anything visible
   * on the display when {@link PowerManager#isInteractive} is false.
   *
   * @param token An identifier for the ambient display suppression.
   * @param suppress If {@code true}, suppresses the ambient display. Otherwise, unsuppresses the
   *     ambient display for the given token.
   */
  @Implementation(minSdk = R)
  @SystemApi
  @RequiresPermission(permission.WRITE_DREAM_STATE)
  protected void suppressAmbientDisplay(String token, boolean suppress) {
    String suppressionToken = Binder.getCallingUid() + "_" + token;
    if (suppress) {
      ambientDisplaySuppressionTokens.add(suppressionToken);
    } else {
      ambientDisplaySuppressionTokens.remove(suppressionToken);
    }
  }

  /**
   * Returns true if {@link #suppressAmbientDisplay(String, boolean)} has been called with any
   * token.
   */
  @Implementation(minSdk = R)
  @SystemApi
  @RequiresPermission(permission.READ_DREAM_STATE)
  protected boolean isAmbientDisplaySuppressed() {
    return !ambientDisplaySuppressionTokens.isEmpty();
  }

  /**
   * Returns last value specified in {@link #setIsRebootingUserspaceSupported(boolean)} or {@code
   * false} by default.
   */
  @Implementation(minSdk = R)
  @SystemApi
  protected boolean isRebootingUserspaceSupported() {
    return isRebootingUserspaceSupported;
  }

  @Implements(PowerManager.WakeLock.class)
  public static class ShadowWakeLock {
    private boolean refCounted = true;
    private int refCount = 0;
    private boolean locked = false;
    private WorkSource workSource = null;
    private int timesHeld = 0;
    private String tag = null;

    @Implementation
    protected void acquire() {
      acquire(0);
    }

    @Implementation
    protected synchronized void acquire(long timeout) {
      ++timesHeld;
      if (refCounted) {
        refCount++;
      } else {
        locked = true;
      }
    }

    // TODO: remove this shadow method once clients are no longer overriding it since it matches the
    // real implementation.
    /** Releases the wake lock. */
    @Implementation
    protected synchronized void release() {
      release(0);
    }

    /** Releases the wake lock. The {@code flags} are ignored. */
    @Implementation
    protected synchronized void release(int flags) {
      if (refCounted) {
        if (--refCount < 0) throw new RuntimeException("WakeLock under-locked");
      } else {
        locked = false;
      }
    }

    @Implementation
    protected synchronized boolean isHeld() {
      return refCounted ? refCount > 0 : locked;
    }

    /**
     * Retrieves if the wake lock is reference counted or not
     *
     * @return Is the wake lock reference counted?
     */
    public boolean isReferenceCounted() {
      return refCounted;
    }

    @Implementation
    protected void setReferenceCounted(boolean value) {
      refCounted = value;
    }

    @Implementation
    protected synchronized void setWorkSource(WorkSource ws) {
      workSource = ws;
    }

    public synchronized WorkSource getWorkSource() {
      return workSource;
    }

    /** Returns how many times the wakelock was held. */
    public int getTimesHeld() {
      return timesHeld;
    }

    /** Returns the tag. */
    @HiddenApi
    @Implementation(minSdk = O)
    public String getTag() {
      return tag;
    }

    /** Sets the tag. */
    private void setTag(String tag) {
      this.tag = tag;
    }
  }
}
