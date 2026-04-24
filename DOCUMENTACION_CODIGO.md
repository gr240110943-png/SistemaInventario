# Documentacion del Codigo

## 1. Estructura general

Todo el codigo esta en el paquete `sistemainventario`.

Archivos principales:

- `SistemaInventario.java`: punto de arranque (`main`).
- `InicioFrame.java`: ventana principal con menu lateral y navegacion entre vistas.
- `InicioPanel.java`: pantalla de inicio con tarjetas/resumen.
- `CatalogoPanel.java`: modulo para CRUD de productos.
- `MovimientosPanel.java`: modulo para registrar movimientos y ver historial.
- `StockPanel.java`: modulo para revisar estado de stock actual.
- `InventarioService.java`: reglas de negocio y validaciones.
- `CsvInventarioRepository.java`: lectura/escritura de `productos.csv` y `movimientos.csv`.
- `Producto.java`: modelo de producto.
- `MovimientoInventario.java`: modelo de movimiento.
- `InventarioException.java`: excepcion de negocio.
- `Interfaz.java`: estilos reutilizables para componentes Swing.
- `NavegadorDashboard.java`: interfaz funcional para cambiar de vista.

## 2. Flujo de inicio

1. `SistemaInventario.main()` crea:
   - `CsvInventarioRepository`
   - `InventarioService`
   - `InicioFrame`
2. `InicioFrame` carga los paneles y muestra `Inicio`.
3. Cada panel usa `InventarioService` para trabajar con datos.

## 3. Flujo de productos

1. El usuario crea/edita en `CatalogoPanel`.
2. `CatalogoPanel` valida campos locales (incluye errores multiples).
3. Si pasa, llama a `InventarioService`.
4. `InventarioService` valida reglas de negocio.
5. Si pasa, persiste con `CsvInventarioRepository`.

## 4. Flujo de movimientos

1. El usuario registra movimiento en `MovimientosPanel`.
2. El panel valida todas las casillas y muestra lista de errores si hay.
3. Si pasa, llama a `InventarioService.registrarMovimiento(...)`.
4. El servicio valida stock/tipo/cantidad y guarda en CSV.
5. Se refrescan inicio, stock, catalogo e historial.

## 5. Validaciones importantes

Para productos:

- `clave`, `nombre`, `categoria`: obligatorios.
- `costo` y `precio`: mayores que 0.
- `stockActual`: mayor o igual a 0.
- `stockMinimo`: mayor que 0.
- `tiempoEntrega`: mayor o igual a 0.
- `demanda`: mayor que 0.
- `clave` no repetida al crear.

Para movimientos:

- `producto`, `tipo`, `fecha`, `motivo`: obligatorios.
- `cantidad`:
  - entrada/salida: mayor que 0
  - ajuste: distinto de 0
- salida no puede exceder stock actual.
- no se permite stock final negativo.

## 6. Como extender facil

Si quieres agregar algo nuevo:

- Regla nueva de negocio: `InventarioService`.
- Campo nuevo en CSV: `CsvInventarioRepository` + modelo.
- Cambio visual: panel correspondiente (`InicioPanel`, `CatalogoPanel`, etc.).
- Estilo visual global: `Interfaz.java`.
