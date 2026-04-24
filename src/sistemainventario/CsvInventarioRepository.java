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
    private final String rutaMovimientos;

    /**
     * Define las rutas de los archivos CSV a utilizar.
     */
    public CsvInventarioRepository(String rutaProductos, String rutaMovimientos) {
        this.rutaProductos = rutaProductos;
        this.rutaMovimientos = rutaMovimientos;
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
        List<MovimientoInventario> movimientos = new ArrayList<>();
        File archivo = new File(rutaMovimientos);
        if (!archivo.exists()) {
            return movimientos;
        }

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Paths.get(rutaMovimientos), StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, formato)) {

            for (CSVRecord record : csvParser) {
                try {
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
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("Id", "ClaveProducto", "Cantidad", "Fecha", "Tipo", "Motivo", "StockAntes", "StockDespues")
                .build();

        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientos), StandardCharsets.UTF_8);
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
        File archivo = new File(rutaMovimientos);
        if (archivo.exists()) {
            return;
        }
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader("Id", "ClaveProducto", "Cantidad", "Fecha", "Tipo", "Motivo", "StockAntes", "StockDespues")
                .build();
        try (Writer writer = Files.newBufferedWriter(Paths.get(rutaMovimientos), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, formato)) {
            csvPrinter.flush();
        }
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
}
