import java.lang.reflect.*;

/** Run through app_process as root; no installed application or system APK edits. */
public final class OverlayProbe {
    public static void main(String[] args) throws Exception {
        Class<?> overlayClass = Class.forName("android.content.om.FabricatedOverlay");
        Object overlay = overlayClass.getConstructor(String.class, String.class)
            .newInstance(args[0], args[1]);
        overlayClass.getMethod("setOwningPackage", String.class).invoke(overlay, "com.android.shell");
        Method dimension = overlayClass.getMethod("setResourceValue", String.class,
            float.class, int.class, String.class);
        for (int i = 2; i < args.length; i += 2) {
            dimension.invoke(overlay, args[1] + ":dimen/" + args[i],
                Float.parseFloat(args[i + 1]), 1, null);
        }
        Class<?> builderClass = Class.forName("android.content.om.OverlayManagerTransaction$Builder");
        Object builder = builderClass.getConstructor().newInstance();
        builderClass.getMethod("registerFabricatedOverlay", overlayClass).invoke(builder, overlay);
        Object identifier = overlayClass.getMethod("getIdentifier").invoke(overlay);
        builderClass.getMethod("setEnabled", Class.forName("android.content.om.OverlayIdentifier"),
            boolean.class, int.class).invoke(builder, identifier, true, 0);
        Object transaction = builderClass.getMethod("build").invoke(builder);
        Object binder = Class.forName("android.os.ServiceManager").getMethod("getService", String.class)
            .invoke(null, "overlay");
        Object manager = Class.forName("android.content.om.IOverlayManager$Stub")
            .getMethod("asInterface", Class.forName("android.os.IBinder")).invoke(null, binder);
        Class.forName("android.content.om.IOverlayManager")
            .getMethod("commit", Class.forName("android.content.om.OverlayManagerTransaction"))
            .invoke(manager, transaction);
        System.out.println("Enabled com.android.shell:" + args[0]);
    }
}
