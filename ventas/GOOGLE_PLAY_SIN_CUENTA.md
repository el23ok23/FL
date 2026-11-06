# Google Play: qué decir y qué hacer sin tener cuenta

Documento interno, solo para vos. Al cliente no se le dice que no tenés cuenta: se le dice que Google Play es opcional y que, si lo quiere, se publica con una cuenta a nombre de su empresa. Lo que sigue es para que sepas qué implica si alguno lo pide.

## Lo que vendés

La **web app**: se instala desde el enlace, en Android y iPhone, con el ícono y el nombre de la empresa. Es el producto completo. Google Play no agrega ninguna función; solo cambia desde dónde se instala.

## Qué decir si preguntan por Google Play

"Se puede, es la misma app. Se publica con una cuenta de desarrollador a nombre de tu empresa, cuesta 25 dólares una vez, Google verifica la identidad, pide una prueba cerrada con 12 personas durante dos semanas y después revisa la app. Entre todo, 45 a 60 días. Yo preparo todo y hacemos el trámite juntos, como segunda etapa, y se cotiza aparte."

Nunca digas "te la subo a Play": no es un botón, y no depende de vos.

## Si una empresa la pide de verdad: dos caminos

**A. Cuenta a nombre de la empresa (recomendado).** La app es de ellos; la cuenta también.
1. El dueño entra a play.google.com/console con una cuenta de Google de la empresa, elige "Organización", paga los 25 dólares con su tarjeta y verifica identidad (DNI del responsable, datos de la empresa y número D-U-N-S, que se pide gratis en dnb.com y tarda hasta 30 días).
2. Te agrega como usuario de la cuenta con permiso para publicar.
3. Vos subís el archivo `.aab` que ya genera GitHub en cada build (`FletabaSalto-release.aab` o el de la empresa, en Releases), cargás la ficha (nombre, descripción, capturas, ícono), la política de privacidad (ya está publicada en la web) y el formulario de seguridad de datos.
4. Prueba cerrada: Google exige 12 probadores durante 14 días seguidos para cuentas nuevas. Los probadores pueden ser el dueño, choferes, familiares y vos.
5. Pedís acceso a producción, Google revisa (2 a 14 días) y publica.

**B. Cuenta a tu nombre.** Solo si vas a publicar varias apps. Cuenta "Personal", 25 dólares, tu DNI, misma prueba cerrada de 14 días. Las apps salen a tu nombre y después se pueden transferir a la cuenta de la empresa. Mientras no tengas una venta que lo justifique, no la abras.

## Lo que ya está listo del lado técnico

- GitHub genera en cada build el `.aab` firmado para Play (y el `.apk` para instalar a mano).
- La política de privacidad está publicada en la web.
- Falta solo lo que depende de la cuenta: la ficha, las capturas y los formularios de la consola.

## Plazos que decís al cliente

Enlace de prueba en 7 días, entrega de la web app en 15 días hábiles, Google Play (si lo piden) 45 a 60 días desde que su cuenta queda verificada. Todo con margen.
