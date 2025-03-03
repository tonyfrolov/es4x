package io.reactiverse.es4x.impl;

import io.vertx.core.json.JsonObject;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.Map;

public class StructuredClone {

  public static Object cloneObject(Value oToBeCloned) {
    return cloneObject(oToBeCloned, new HashMap<>());
  }

  private static Object cloneObject(Value oToBeCloned, Map<Object, Object> dejaVu) {

    if (dejaVu.containsKey(oToBeCloned)) {
      return dejaVu.get(oToBeCloned);
    }

    // null
    if (oToBeCloned == null || oToBeCloned.isNull()) {
      return null;
    }

    // primitives

    if (oToBeCloned.isNumber()) {
      return oToBeCloned.as(Number.class);
    }

    if (oToBeCloned.isBoolean()) {
      return oToBeCloned.asBoolean();
    }

    if (oToBeCloned.isString()) {
      return oToBeCloned.asString();
    }

    // temporal types

    if (oToBeCloned.isDate()) {
      return oToBeCloned.asDate();
    }

    if (oToBeCloned.isDuration()) {
      return oToBeCloned.asDuration();
    }

    if (oToBeCloned.isInstant()) {
      return oToBeCloned.asInstant();
    }

    if (oToBeCloned.isTime()) {
      return oToBeCloned.asTime();
    }

    if (oToBeCloned.isTimeZone()) {
      return oToBeCloned.asTimeZone();
    }

    // others

    Value fConstr = oToBeCloned.getMember("constructor");
    String fConstrName = fConstr.getMember("name").asString();
    Object oClone = null;

    switch (fConstrName) {
      case "Object":
        oClone = new JsonObject();
        dejaVu.put(oToBeCloned, oClone);
        for (String key : oToBeCloned.getMemberKeys()) {
          ((JsonObject) oClone).put(key, cloneObject(oToBeCloned.getMember(key), dejaVu));
        }
        break;

//      case RegExp:
//        oClone = new fConstr(oToBeCloned.source, "g".substr(0, Number(oToBeCloned.global)) + "i".substr(0, Number(oToBeCloned.ignoreCase)) + "m".substr(0, Number(oToBeCloned.multiline)));
//        break;
//      case Date:
//        oClone = new fConstr(oToBeCloned.getTime());
//        break;
//      // etc.
//      default:
//        if (Buffer.isBuffer(oToBeCloned)) {
//          oClone = new Buffer(oToBeCloned.length);
//          oToBeCloned.copy(oClone);
//        } else if (oToBeCloned instanceof Error) {
//          oClone = new Error(oToBeCloned.message);
//        } else {
//          oClone = new fConstr();
//          cloned.push(oToBeCloned); clonedpairs.push(oClone);
//          for (var sProp in oToBeCloned) { oClone[sProp] = clone(oToBeCloned[sProp], cloned, clonedpairs); }
//        }
    }
    return oClone;
  }
}
