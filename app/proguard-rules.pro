# La app no usa reflexión ni serialización por reflexión: alcanza con las reglas por defecto.
# Sacamos los logs en release para achicar y acelerar.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
