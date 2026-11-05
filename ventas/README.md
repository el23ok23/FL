# Ventas · Fletaba marca blanca para empresas de fletes de Salto

- `PROPUESTA_COMERCIAL.md`: qué recibe la empresa, precio único (sin comisión ni abono), proceso y preguntas frecuentes.
- `ENTREGA_AL_CLIENTE.md`: instructivo para el dueño: cómo cambia precios, nombre, colores y logo desde la app.
- `CONFIGURAR_PANEL_DEL_DUENO.md`: cómo armamos la planilla y el Apps Script gratuito que sincroniza esos cambios (una vez por empresa).
- `folleto.html`: la misma propuesta en una página para mandar como enlace o imprimir.
- `flyer-empresas-1080x1350.png`: imagen para mandar por WhatsApp o publicar.
- `GUION_DE_VENTA.md`: a quién, cómo abrir, demo de 2 minutos, objeciones y cierre.
- `CONTRATO_MODELO.md`: contrato de servicio para adaptar con un contador o abogado.

Alta de una empresa nueva (crea la app, la web y la publicación automática):

```
python3 scripts/nueva_empresa.py --slug donpedro --nombre "Fletes Don Pedro" --whatsapp 5492474123456 --origen salto --color "#0E7C4A"
git add -A && git commit -m "Nueva empresa: Fletes Don Pedro" && git push
```

En unos minutos: `https://github.com/el23ok23/FL/releases/download/ultima-version/FletesDonPedro.apk` y `https://el23ok23.github.io/FL/donpedro/`.
Antes, crear la planilla y el Apps Script del dueño (`CONFIGURAR_PANEL_DEL_DUENO.md`) y pasar la dirección `/exec` con `--precios-url`. Para que los pedidos le lleguen solos, cargar su clave de CallMeBot en `app/src/donpedro/assets/config.json` y en el bloque CONFIG de `docs/donpedro/index.html`.
