package sistemainventario;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Capa de negocio principal del sistema.
 *
 * Esta clase centraliza reglas como:
 * - Validaciones de productos y movimientos.
 * - Calculo de estado de stock.
 * - Persistencia (a traves del repositorio CSV).
 */
public class InventarioService {

    private static final DateTimeFormatter FORMATO_ID = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CsvInventarioRepository repository;
    private final Map<String, String> categorias = new LinkedHashMap<>();

    private List<Producto> productos = new ArrayList<>();
    private List<MovimientoEncabezado> movimientosEncabezado = new ArrayList<>();
    private List<MovimientoDetalle> movimientosDetalle = new ArrayList<>();
    private ParametrosConfiguracion configuracion;
    private int siguienteNumeroMovimiento = 1;

    /**
     * Crea el servicio y carga la informacion desde CSV.
     */
    public InventarioService(CsvInventarioRepository repository) throws InventarioException {
        this.repository = repository;
        inicializarCategorias();
        recargar();
    }

    /**
     * Recarga todos los datos desde disco.
     */
    public final void recargar() throws InventarioException {
        try {
            repository.asegurarArchivoMovimientos();
            productos = repository.cargarProductos();
            configuracion = repository.cargarConfiguracion();
            // Nuevo esquema
            movimientosEncabezado = repository.cargarMovimientosEncabezado();
            movimientosDetalle = repository.cargarMovimientosDetalle();
            siguienteNumeroMovimiento = calcularSiguienteNumeroMovimiento();
        } catch (IOException ex) {
            throw new InventarioException("No se pudo cargar el inventario: " + ex.getMessage());
        }
    }

    public Map<String, String> getCategorias() {
        return Collections.unmodifiableMap(categorias);
    }

    /**
     * Devuelve los productos ordenados alfabeticamente por nombre.
     */
    public List<Producto> obtenerProductosOrdenados() {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER));
        return copia;
    }

    public List<Producto> filtrarProductos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return obtenerProductosOrdenados();
        }
        String criterio = filtro.toLowerCase().trim();
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : productos) {
            String categoriaNombre = obtenerNombreCategoria(p.getCategoriaClave()).toLowerCase();
            if (p.getClave().toLowerCase().contains(criterio)
                    || p.getNombre().toLowerCase().contains(criterio)
                    || categoriaNombre.contains(criterio)) {
                resultado.add(p);
            }
        }
        resultado.sort(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }

    public Producto buscarProductoPorClave(String clave) {
        if (clave == null) {
            return null;
        }
        for (Producto p : productos) {
            if (p.getClave().equalsIgnoreCase(clave.trim())) {
                return p;
            }
        }
        return null;
    }

    public String obtenerNombreCategoria(String clave) {
        return categorias.getOrDefault(clave, "Desconocida");
    }

    /**
     * Busca la clave interna de categoria a partir del nombre mostrado.
     */
    public String obtenerClaveCategoria(String nombre) {
        for (Map.Entry<String, String> item : categorias.entrySet()) {
            if (item.getValue().equals(nombre)) {
                return item.getKey();
            }
        }
        return "";
    }

    public void registrarProducto(Producto nuevo) throws InventarioException {
        validarProducto(nuevo, false);
        productos.add(nuevo);
        persistirProductos();
    }

    /**
     * Actualiza un producto existente por su clave.
     */
    public void actualizarProducto(Producto actualizado) throws InventarioException {
        validarProducto(actualizado, true);
        Producto existente = buscarProductoPorClave(actualizado.getClave());
        if (existente == null) {
            throw new InventarioException("No existe el producto con clave: " + actualizado.getClave());
        }
        if ("Inactivo".equalsIgnoreCase(existente.getEstado())) {
            throw new InventarioException("No se puede editar un producto inactivo. Activelo primero.");
        }

        existente.setNombre(actualizado.getNombre());
        existente.setCategoriaClave(actualizado.getCategoriaClave());
        existente.setCosto(actualizado.getCosto());
        existente.setPrecio(actualizado.getPrecio());
        existente.setStockActual(actualizado.getStockActual());
        existente.setStockMinimo(actualizado.getStockMinimo());
        existente.setTiempoEntrega(actualizado.getTiempoEntrega());
        existente.setDemanda(actualizado.getDemanda());
        existente.setEstado(actualizado.getEstado());

        persistirProductos();
    }

    public void cambiarEstadoProducto(String clave) throws InventarioException {
        Producto p = buscarProductoPorClave(clave);
        if (p == null) {
            throw new InventarioException("No existe el producto seleccionado.");
        }
        p.setEstado("Activo".equalsIgnoreCase(p.getEstado()) ? "Inactivo" : "Activo");
        persistirProductos();
    }

    /**
     * Registra un movimiento (encabezado + multiples detalles) y actualiza stock.
     *
     * Reglas:
     * - Salida: no permite si stock insuficiente.
     * - Producto Inactivo: no permite Entrada/Salida; si permite Ajuste.
     * - Ajuste: requiere motivo (comentario) y la cantidad no puede ser 0.
     */
    public MovimientoEncabezado registrarMovimiento(String tipo, LocalDateTime fecha, String motivo, List<MovimientoLinea> lineas)
            throws InventarioException {

        List<String> errores = new ArrayList<>();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }

        String tipoNorm = capitalizar(tipo);
        if (tipoNorm.isBlank()
                || !(tipoNorm.equalsIgnoreCase("Entrada") || tipoNorm.equalsIgnoreCase("Salida") || tipoNorm.equalsIgnoreCase("Ajuste"))) {
            errores.add("Tipo: valor no valido.");
        }

        String motivoLimpio = motivo == null ? "" : motivo.trim();
        // Solo validamos que no esté vacío si el tipo de movimiento es "Ajuste"
        if ("Ajuste".equalsIgnoreCase(tipoNorm) && motivoLimpio.isEmpty()) {
            errores.add("Motivo: es obligatorio para los ajustes.");
        }
        
        if (lineas == null || lineas.isEmpty()) {
            errores.add("Detalle: agrega al menos un producto.");
        }

        if (!errores.isEmpty()) {
            throw new InventarioException(construirMensajeErrores("No se pudo registrar el movimiento. Revisa:", errores));
        }

        // Validar y calcular efectos sin mutar estado (operacion atomica)
        List<MovimientoDetalle> detallesARegistrar = new ArrayList<>();
        List<String> advertenciasSobreinventario = new ArrayList<>();

        String idMovimiento = generarIdMovimiento();
        int numeroMovimiento = siguienteNumeroMovimiento;
        for (MovimientoLinea linea : lineas) {
            if (linea == null || linea.getClaveProducto() == null || linea.getClaveProducto().isBlank()) {
                errores.add("Producto: clave vacia en detalle.");
                continue;
            }
            Producto producto = buscarProductoPorClave(linea.getClaveProducto());
            if (producto == null) {
                errores.add("Producto: no existe (" + linea.getClaveProducto() + ").");
                continue;
            }

            boolean inactivo = "Inactivo".equalsIgnoreCase(producto.getEstado());
            if (inactivo && ("Entrada".equalsIgnoreCase(tipoNorm) || "Salida".equalsIgnoreCase(tipoNorm))) {
                errores.add("Producto " + producto.getClave() + ": esta inactivo. Solo se permite ajuste.");
                continue;
            }

          int cantidad = linea.getCantidad();
            int stockAntes = producto.getStockActual(); // Lo movemos arriba para usarlo en el Ajuste
            int ajuste = 0;

            if ("Entrada".equalsIgnoreCase(tipoNorm)) {
                if (cantidad <= 0) {
                    errores.add("Cantidad (" + producto.getClave() + "): para entrada debe ser mayor que 0.");
                    continue;
                }
                ajuste = cantidad;
            } else if ("Salida".equalsIgnoreCase(tipoNorm)) {
                if (cantidad <= 0) {
                    errores.add("Cantidad (" + producto.getClave() + "): para salida debe ser mayor que 0.");
                    continue;
                }
                ajuste = -cantidad;
            } else if ("Ajuste".equalsIgnoreCase(tipoNorm)) {
                if (cantidad < 0) { 
                    // Para un ajuste, normalmente permitimos 0 (por si se perdió todo), pero no negativos.
                    errores.add("Cantidad (" + producto.getClave() + "): para ajuste no puede ser negativa.");
                    continue;
                }
                // Calculamos la diferencia exacta para que el stock final termine siendo la 'cantidad' ingresada
                ajuste = cantidad - stockAntes; 
            }

            int stockDespues = stockAntes + ajuste;
            if ("Salida".equalsIgnoreCase(tipoNorm) && cantidad > stockAntes) {
                errores.add("Cantidad (" + producto.getClave() + "): supera el stock disponible (" + stockAntes + ").");
                continue;
            }
            if (stockDespues < 0) {
                errores.add("Stock (" + producto.getClave() + "): el movimiento dejaria stock negativo.");
                continue;
            }

            if ("Entrada".equalsIgnoreCase(tipoNorm) && producto.getStockMinimo() > 0
                    && (stockAntes + cantidad) > (producto.getStockMinimo() * 3)) {
                advertenciasSobreinventario.add("Advertencia (" + producto.getClave() + "): la entrada dejaria el stock en "
                        + (stockAntes + cantidad) + " (> 3x minimo " + producto.getStockMinimo() + ").");
            }

            detallesARegistrar.add(new MovimientoDetalle(
                    idMovimiento,
                    producto.getClave(),
                    ajuste,
                    stockAntes,
                    stockDespues
            ));
        }

        if (!errores.isEmpty()) {
            throw new InventarioException(construirMensajeErrores("No se pudo registrar el movimiento. Revisa:", errores));
        }

        // Mutar stock y persistir
        for (MovimientoDetalle d : detallesARegistrar) {
            Producto producto = buscarProductoPorClave(d.getClaveProducto());
            if (producto != null) {
                producto.setStockActual(d.getStockDespues());
            }
        }

        MovimientoEncabezado encabezado = new MovimientoEncabezado(
                idMovimiento,
                numeroMovimiento,
                fecha,
                tipoNorm,
                motivoLimpio
        );
        movimientosEncabezado.add(encabezado);
        movimientosDetalle.addAll(detallesARegistrar);
        siguienteNumeroMovimiento++;

        persistirTodo();
        return encabezado;
    }

    public List<String> previsualizarAdvertenciasSobreinventario(String tipo, List<MovimientoLinea> lineas) {
        String tipoNorm = capitalizar(tipo);
        if (!"Entrada".equalsIgnoreCase(tipoNorm) || lineas == null) {
            return List.of();
        }
        List<String> advertencias = new ArrayList<>();
        for (MovimientoLinea l : lineas) {
            if (l == null) {
                continue;
            }
            Producto p = buscarProductoPorClave(l.getClaveProducto());
            if (p == null) {
                continue;
            }
            if (l.getCantidad() <= 0) {
                continue;
            }
            int stockDespues = p.getStockActual() + l.getCantidad();
            if (p.getStockMinimo() > 0 && stockDespues > p.getStockMinimo() * 3) {
                advertencias.add("Advertencia (" + p.getClave() + "): la entrada dejaria el stock en "
                        + stockDespues + " (> 3x minimo " + p.getStockMinimo() + ").");
            }
        }
        return advertencias;
    }

    public List<MovimientoInventario> obtenerMovimientos(String claveProducto) {
        List<MovimientoInventario> lista = new ArrayList<>();
        // Nuevo esquema: join encabezado + detalles
        for (MovimientoDetalle d : movimientosDetalle) {
            if (claveProducto == null || claveProducto.isBlank()
                    || d.getClaveProducto().equalsIgnoreCase(claveProducto)) {
                MovimientoEncabezado enc = buscarEncabezado(d.getIdMovimiento());
                if (enc != null) {
                    lista.add(new MovimientoInventario(
                            enc.getId(),
                            d.getClaveProducto(),
                            d.getCantidad(),
                            enc.getFecha(),
                            enc.getTipo(),
                            enc.getMotivo(),
                            d.getStockAntes(),
                            d.getStockDespues()
                    ));
                }
            }
        }

        lista.sort(Comparator
                .comparing(MovimientoInventario::getFecha)
                .reversed()
                .thenComparing(MovimientoInventario::getId, Comparator.reverseOrder()));

        return lista;
    }

    public int obtenerNumeroMovimiento(String idMovimiento) {
        MovimientoEncabezado enc = buscarEncabezado(idMovimiento);
        return enc == null ? 0 : enc.getNumeroMovimiento();
    }

    public String calcularEstadoStock(Producto producto) {
        if (producto.getStockActual() <= 0) {
            return "Agotado";
        }
        if (producto.getStockMinimo() > producto.getStockActual()) {
            return "Stock bajo";
        }
        if (producto.getStockMinimo() > 0 && producto.getStockActual() > producto.getStockMinimo() * 3) {
            return "Sobreinventario";
        }
        return "Normal";
    }

    public int contarProductos() {
        return productos.size();
    }

    public int contarAgotados() {
        int total = 0;
        for (Producto p : productos) {
            if ("Agotado".equals(calcularEstadoStock(p))) {
                total++;
            }
        }
        return total;
    }

    public int contarStockBajo() {
        int total = 0;
        for (Producto p : productos) {
            if ("Stock bajo".equals(calcularEstadoStock(p))) {
                total++;
            }
        }
        return total;
    }

    public int contarSobreinventario() {
        int total = 0;
        for (Producto p : productos) {
            if ("Sobreinventario".equals(calcularEstadoStock(p))) {
                total++;
            }
        }
        return total;
    }

    public int contarMovimientos() {
        return movimientosEncabezado.size();
    }

    /**
     * Numero consecutivo que se asignara al siguiente movimiento registrado.
     */
    public int obtenerSiguienteNumeroMovimiento() {
        return siguienteNumeroMovimiento;
    }

    public ParametrosConfiguracion obtenerConfiguracion() {
        return configuracion;
    }

    public void guardarConfiguracion(ParametrosConfiguracion nuevaConfiguracion) throws InventarioException {
        validarConfiguracion(nuevaConfiguracion);
        configuracion = nuevaConfiguracion;
        try {
            repository.guardarConfiguracion(configuracion);
        } catch (IOException ex) {
            throw new InventarioException("No se pudo guardar configuracion: " + ex.getMessage());
        }
    }

    private void validarConfiguracion(ParametrosConfiguracion cfg) throws InventarioException {
        List<String> errores = new ArrayList<>();
        if (cfg == null) {
            throw new InventarioException("Configuracion invalida.");
        }
        if (cfg.getCostoPedido() <= 0) {
            errores.add("Costo por pedido: debe ser mayor que 0.");
        }
        if (cfg.getCostoMantenimiento() <= 0) {
            errores.add("Costo de mantenimiento: debe ser mayor que 0.");
        }
        if (cfg.getTiempoEntrega() <= 0) {
            errores.add("Tiempo de entrega: debe ser un numero entero mayor que 0.");
        }
        if (!errores.isEmpty()) {
            throw new InventarioException(construirMensajeErrores("No se pudo guardar la configuracion. Revisa:", errores));
        }
    }

    private void validarProducto(Producto producto, boolean esEdicion) throws InventarioException {
        List<String> errores = new ArrayList<>();

        if (producto == null) {
            throw new InventarioException("Producto invalido.");
        }
        if (producto.getClave() == null || producto.getClave().trim().isEmpty()) {
            errores.add("Clave: esta vacia.");
        }
        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            errores.add("Nombre: esta vacio.");
        }
        if (producto.getCategoriaClave() == null || producto.getCategoriaClave().trim().isEmpty()) {
            errores.add("Categoria: no seleccionada.");
        }
        if (producto.getCosto() <= 0) {
            errores.add("Costo: debe ser mayor que 0.");
        }
        if (producto.getPrecio() <= 0) {
            errores.add("Precio: debe ser mayor que 0.");
        }
        if (producto.getStockActual() < 0) {
            errores.add("Stock actual: no puede ser negativo.");
        }
        if (producto.getStockMinimo() <= 0) {
            errores.add("Stock minimo: debe ser mayor que 0.");
        }
        if (producto.getTiempoEntrega() <= 0) {
            errores.add("Tiempo de entrega: debe ser 1 o mayor.");
        }
        if (producto.getDemanda() <= 0) {
            errores.add("Demanda: debe ser mayor que 0.");
        }
        if (!esEdicion && buscarProductoPorClave(producto.getClave()) != null) {
            errores.add("Clave: ya existe.");
        }

        if (!errores.isEmpty()) {
            throw new InventarioException(construirMensajeErrores("No se pudo guardar el producto. Revisa:", errores));
        }
    }

    private void persistirProductos() throws InventarioException {
        try {
            repository.guardarProductos(productos);
        } catch (IOException ex) {
            throw new InventarioException("No se pudo guardar productos: " + ex.getMessage());
        }
    }

    private void persistirTodo() throws InventarioException {
        try {
            repository.guardarProductos(productos);
            repository.guardarMovimientosEncabezado(movimientosEncabezado);
            repository.guardarMovimientosDetalle(movimientosDetalle);
        } catch (IOException ex) {
            throw new InventarioException("No se pudieron guardar cambios: " + ex.getMessage());
        }
    }

    private String generarIdMovimiento() {
        int correlativo = movimientosEncabezado.size() + 1;
        String id;
        do {
            id = "MOV-" + LocalDateTime.now().format(FORMATO_ID) + "-" + String.format("%03d", correlativo++);
        } while (existeMovimiento(id));
        return id;
    }

    private boolean existeMovimiento(String id) {
        for (MovimientoEncabezado m : movimientosEncabezado) {
            if (m.getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private MovimientoEncabezado buscarEncabezado(String idMovimiento) {
        if (idMovimiento == null) {
            return null;
        }
        for (MovimientoEncabezado e : movimientosEncabezado) {
            if (idMovimiento.equalsIgnoreCase(e.getId())) {
                return e;
            }
        }
        return null;
    }

    private int calcularSiguienteNumeroMovimiento() {
        int max = 0;
        for (MovimientoEncabezado e : movimientosEncabezado) {
            if (e.getNumeroMovimiento() > max) {
                max = e.getNumeroMovimiento();
            }
        }
        return max + 1;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String limpio = texto.trim().toLowerCase();
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }

    /**
     * Construye un mensaje con multiples errores para mostrarlos en una sola ventana.
     */
    private String construirMensajeErrores(String encabezado, List<String> errores) {
        StringBuilder mensaje = new StringBuilder(encabezado).append(System.lineSeparator());
        for (String error : errores) {
            mensaje.append("- ").append(error).append(System.lineSeparator());
        }
        return mensaje.toString().trim();
    }

    private void inicializarCategorias() {
        categorias.put("ELEC01", "Computadoras y laptops");
        categorias.put("ELEC02", "Componentes de PC");
        categorias.put("ELEC03", "Perifericos");
        categorias.put("ELEC04", "Monitores");
        categorias.put("ELEC05", "Impresoras y escaneres");
        categorias.put("ELEC06", "Redes y conectividad");
        categorias.put("ELEC07", "Almacenamiento");
        categorias.put("ELEC08", "Accesorios para celulares");
        categorias.put("ELEC09", "Smartphones y tablets");
        categorias.put("ELEC10", "Audio y sonido");
        categorias.put("ELEC11", "Video y entretenimiento");
        categorias.put("ELEC12", "Energia y proteccion");
        categorias.put("ELEC13", "Camaras y videovigilancia");
        categorias.put("ELEC14", "Gadgets y wearables");
        categorias.put("ELEC15", "Consumibles");
    }
}
