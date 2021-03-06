package org.swellrt.beta.client.js.editor;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Wrapper for native object containing editor configuration 
 * properties. 
 * <p><br>
 * To be effectively used, add a script section before
 * loading SwellRT javascript declaring the config object:
 * <p><br>
 * <code>
 *  __swellrt_editor_config = {
 *      enableLog: true;
 *      ...
 *    }
 * </code>
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
@JsType(isNative = true, name="__swellrt_editor_config", namespace = JsPackage.GLOBAL)
public class SEditorConfig {
  
  @JsProperty
  public static native Boolean getEnableLog();

  @JsProperty
  public static native Boolean getDebugDialog();

  @JsProperty
  public static native Boolean getUndo();
  
  @JsProperty
  public static native Boolean getFancyCursorBias();
  
  @JsProperty
  public static native Boolean getSemanticCopyPaste();
      
  @JsProperty
  public static native Boolean getWhitelistEditor();

  @JsProperty
  public static native Boolean getWebkitComposition();


  /*
   * Methods with secure fall back value
   * if property is not available
   */
  
  @JsOverlay
  public final static boolean enableLog() {
    boolean DEFAULT = false;
    try {
      return getEnableLog() != null ? getEnableLog() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
  @JsOverlay
  public final static boolean debugDialog() {
    boolean DEFAULT = false;
    try {
      return getDebugDialog() != null ? getDebugDialog() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
  @JsOverlay
  public final static boolean undo() {
    boolean DEFAULT = true;
    try {
      return getUndo() != null ? getUndo() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }

  @JsOverlay
  public final static boolean fancyCursorBias() {
    boolean DEFAULT = true;
    try {
      return getFancyCursorBias() != null ? getFancyCursorBias() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
  @JsOverlay
  public final static boolean semanticCopyPaste() {
    boolean DEFAULT = false;
    try {
      return getSemanticCopyPaste() != null ? getSemanticCopyPaste() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
  @JsOverlay
  public final static boolean whitelistEditor() {
    boolean DEFAULT = false;
    try {
      return getWhitelistEditor() != null ? getWhitelistEditor() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
  @JsOverlay
  public final static boolean webkitComposition() {
    boolean DEFAULT = false;
    try {
      return getWebkitComposition() != null ? getWebkitComposition() : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
}
