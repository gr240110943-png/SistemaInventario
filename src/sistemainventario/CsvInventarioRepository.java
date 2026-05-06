package sistemainventario;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de persistencia en archivos CSV.
 *
 * Se encarga exclusivamente de leer y escribir:
 * - productos.csv
 * - movimientos.csv
 */
public class CsvInventarioRepository {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_SEGUNDOS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String rutaProductos;
    private final String rutaMovimientosLegacy;
    private final String rutaMovimientosEncabezado;
    private final String rutaMovimientosDetalle;

    /**
     * Define las rutas de los archivos CSV a utilizar.
     */
    public CsvInventarioRepository(String rutaProductos, String rutaMovimientos) {
        this.rutaProductos = rutaProductos;
        // Compatibilidad:
        // - rutaMovimientosLegacy: archivo viejo (1 tabla) si existe
        // - rutaMovimientosEncabezado / rutaMovimientosDetalle: nuevo esquema
        this.rutaMovimientosLegacy = rutaMovimientos;
        this.rutaMovimientosEncabezado = derivarRutaEncabezado(rutaMovimientos);
        this.rutaMovimientosDetalle = derivarRutaDetalle(rutaMovimientos);
    }

    /**
     * Carga todos los productos desde productos.csv.
     */
    public List<Producto> cargarProductos() throws IOException {
        List<Producto> productos = new ArrayList<>();
        File archivo = new File(rutaProductos);
        if (!archivo.exists()) {
            return productos;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Paths.get(rutaProductos), StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, formato)) {

            for (CSVRecord record : csvParser) {
                Producto producto = new Producto(
                        record.get(0).trim(),
                        record.get(1).trim(),
                        record.get(2).trim(),
                        record.get(9).trim(),
                        Double.parseDouble(record.get(3).trim()),
                        Double.parseDouble(record.get(4).trim()),
                        Integer.parseInt(record.get(5).trim()),
                        Integer.parseInt(record.get(6).trim()),
                        Integer.parseInt(record.get(7).trim()),
                        Integer.parseInt(record.get(8).trim())
                );
                productos.add(producto);
            }
        }
        return productos;
    }

    /**
     * Guarda toda la lista de productos en productos.csv.
     */
    public void guardarProductos(List<Producto> productos) throws IOException {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("Clave", "Nombre", "ClaveCategoria", "Costo", "Precio",
                        "StockActual", "StockMinimo", "TiempoEntrega", "Demanda", "Estado")
                .build();

        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaProductos), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {

            for (Producto p : productos) {
                csvPrinter.printRecord(
                        p.getClave(),
                        p.getNombre(),
                        p.getCategoriaClave(),
                        p.getCosto(),
                        p.getPrecio(),
                        p.getStockActual(),
                        p.getStockMinimo(),
                        p.getTiempoEntrega(),
                        p.getDemanda(),
                        p.getEstado()
                );
            }
            csvPrinter.flush();
        }
    }

    /**
     * Carga todos los movimientos desde movimientos.csv.
     */
    public List<MovimientoInventario> cargarMovimientos() throws IOException {
        // Backward-compat: intenta leer el formato viejo de una sola tabla (movimientos.csv)
        // para no romper si hay datos previos.
        List<MovimientoInventario> movimientos = new ArrayList<>();
        File archivo = new File(rutaMovimientosLegacy);
        if (!archivo.exists()) {
            return movimientos;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Paths.get(rutaMovimientosLegacy), StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, formato)) {

            for (CSVRecord record : csvParser) {
                try {
                    // Si el archivo es el nuevo encabezado, NO cumple con estas columnas y se ignora
                    if (record.size() < 8) {
                        continue;
                    }
                    MovimientoInventario movimiento = new MovimientoInventario(
                            record.get(0).trim(),
                            record.get(1).trim(),
                            Integer.parseInt(record.get(2).trim()),
                            parseFecha(record.get(3).trim()),
                            record.get(4).trim(),
                            record.get(5).trim(),
                            Integer.parseInt(record.get(6).trim()),
                            Integer.parseInt(record.get(7).trim())
                    );
                    movimientos.add(movimiento);
                } catch (Exception ignored) {
                    // Registro mal formado: se ignora para no romper carga completa.
                }
            }
        }
        return movimientos;
    }

    /**
     * Guarda toda la lista de movimientos en movimientos.csv.
     */
    public void guardarMovimientos(List<MovimientoInventario> movimientos) throws IOException {
        // Mantener metodo por compatibilidad. En el nuevo esquema no se usa.
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("Id", "ClaveProducto", "Cantidad", "Fecha", "Tipo", "Motivo", "StockAntes", "StockDespues")
                .build();

        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientosLegacy), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {

            for (MovimientoInventario m : movimientos) {
                csvPrinter.printRecord(
                        m.getId(),
                        m.getClaveProducto(),
                        m.getCantidad(),
                        m.getFecha().format(FORMATO_FECHA),
                        m.getTipo(),
                        m.getMotivo(),
                        m.getStockAntes(),
                        m.getStockDespues()
                );
            }
            csvPrinter.flush();
        }
    }

    /**
     * Si no existe el archivo de movimientos, lo crea con encabezados.
     */
    public void asegurarArchivoMovimientos() throws IOException {
        asegurarArchivoMovimientosEncabezado();
        asegurarArchivoMovimientosDetalle();
    }

    public void asegurarArchivoMovimientosEncabezado() throws IOException {
        File archivo = new File(rutaMovimientosEncabezado);
        if (archivo.exists()) {
            return;
        }
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("IdMovimiento", "NumeroMovimiento", "Fecha", "Tipo", "Motivo")
                .build();
        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientosEncabezado), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {
            csvPrinter.flush();
        }
    }

    public void asegurarArchivoMovimientosDetalle() throws IOException {
        File archivo = new File(rutaMovimientosDetalle);
        if (archivo.exists()) {
            return;
        }
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("IdMovimiento", "ClaveProducto", "Cantidad", "StockAntes", "StockDespues")
                .build();
        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientosDetalle), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {
            csvPrinter.flush();
        }
    }

    public List<MovimientoEncabezado> cargarMovimientosEncabezado() throws IOException {
        List<MovimientoEncabezado> lista = new ArrayList<>();
        File archivo = new File(rutaMovimientosEncabezado);
        if (!archivo.exists()) {
            return lista;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Paths.get(rutaMovimientosEncabezado), StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, formato)) {
            for (CSVRecord record : csvParser) {
                try {
                    // Nuevo esquema: 5 columnas (IdMovimiento, NumeroMovimiento, Fecha, Tipo, Motivo)
                    if (record.size() < 4) {
                        continue;
                    }
                    // Si detectamos el esquema viejo (8 columnas), lo ignoramos aqui.
                    if (record.size() >= 8 && "ClaveProducto".equalsIgnoreCase(record.get(1).trim())) {
                        continue;
                    }
                    int numero = 0;
                    LocalDateTime fecha;
                    String tipo;
                    String motivo;
                    if (record.size() >= 5) {
                        numero = parseEnteroSeguro(record.get(1).trim());
                        fecha = parseFecha(record.get(2).trim());
                        tipo = record.get(3).trim();
                        motivo = record.get(4).trim();
                    } else {
                        // Compatibilidad con archivo encabezado anterior (sin NumeroMovimiento)
                        fecha = parseFecha(record.get(1).trim());
                        tipo = record.get(2).trim();
                        motivo = record.get(3).trim();
                    }
                    MovimientoEncabezado enc = new MovimientoEncabezado(
                            record.get(0).trim(),
                            numero,
                            fecha,
                            tipo,
                            motivo
                    );
                    lista.add(enc);
                } catch (Exception ignored) {
                    // ignora registros mal formados
                }
            }
        }
        return lista;
    }

    public List<MovimientoDetalle> cargarMovimientosDetalle() throws IOException {
        List<MovimientoDetalle> lista = new ArrayList<>();
        File archivo = new File(rutaMovimientosDetalle);
        if (!archivo.exists()) {
            return lista;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Paths.get(rutaMovimientosDetalle), StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, formato)) {
            for (CSVRecord record : csvParser) {
                try {
                    if (record.size() < 5) {
                        continue;
                    }
                    MovimientoDetalle det = new MovimientoDetalle(
                            record.get(0).trim(),
                            record.get(1).trim(),
                            Integer.parseInt(record.get(2).trim()),
                            Integer.parseInt(record.get(3).trim()),
                            Integer.parseInt(record.get(4).trim())
                    );
                    lista.add(det);
                } catch (Exception ignored) {
                    // ignora registros mal formados
                }
            }
        }
        return lista;
    }

    public void guardarMovimientosEncabezado(List<MovimientoEncabezado> encabezados) throws IOException {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("IdMovimiento", "NumeroMovimiento", "Fecha", "Tipo", "Motivo")
                .build();

        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientosEncabezado), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {
            for (MovimientoEncabezado e : encabezados) {
                csvPrinter.printRecord(
                        e.getId(),
                        e.getNumeroMovimiento(),
                        e.getFecha().format(FORMATO_FECHA),
                        e.getTipo(),
                        e.getMotivo()
                );
            }
            csvPrinter.flush();
        }
    }

    public void guardarMovimientosDetalle(List<MovimientoDetalle> detalles) throws IOException {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("IdMovimiento", "ClaveProducto", "Cantidad", "StockAntes", "StockDespues")
                .build();

        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientosDetalle), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {
            for (MovimientoDetalle d : detalles) {
                csvPrinter.printRecord(
                        d.getIdMovimiento(),
                        d.getClaveProducto(),
                        d.getCantidad(),
                        d.getStockAntes(),
                        d.getStockDespues()
                );
            }
            csvPrinter.flush();
        }
    }

    private static String derivarRutaDetalle(String rutaEncabezado) {
        if (rutaEncabezado == null || rutaEncabezado.isBlank()) {
            return "movimientos_detalle.csv";
        }
        String r = rutaEncabezado.trim();
        int idx = r.lastIndexOf('.');
        if (idx > 0) {
            return r.substring(0, idx) + "_detalle" + r.substring(idx);
        }
        return r + "_detalle.csv";
    }

    private static String derivarRutaEncabezado(String rutaMovimientos) {
        if (rutaMovimientos == null || rutaMovimientos.isBlank()) {
            return "movimientos_encabezado.csv";
        }
        String r = rutaMovimientos.trim();
        int idx = r.lastIndexOf('.');
        if (idx > 0) {
            return r.substring(0, idx) + "_encabezado" + r.substring(idx);
        }
        return r + "_encabezado.csv";
    }

    /**
     * Intenta parsear la fecha con distintos formatos compatibles.
     */
    private LocalDateTime parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(valor, FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(valor, FORMATO_FECHA_SEGUNDOS);
            } catch (DateTimeParseException ex2) {
                try {
                    return LocalDateTime.parse(valor);
                } catch (DateTimeParseException ex3) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    private static int parseEnteroSeguro(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
