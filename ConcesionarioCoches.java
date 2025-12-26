import java.util.Random;
import java.util.concurrent.Semaphore;

public class ConcesionarioCoches {

    public static void main(String[] args) {
        int numeroVehiculos = 4;
        int numeroClientes = 9;

        VehiculosExposicion exposicion = new VehiculosExposicion(numeroVehiculos);

        Thread[] clientes = new Thread[numeroClientes];

        for (int i = 0; i < numeroClientes; i++) {
            String nombreCliente = "cliente_" + (i + 1);
            clientes[i] = new Cliente(nombreCliente, exposicion);
            clientes[i].start();
        }

        for (int i = 0; i < numeroClientes; i++) {
            try {
                clientes[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Simulación finalizada.");
    }
}

class VehiculosExposicion {

    private final Semaphore semaforo;
    private final boolean[] vehiculosOcupados;
    private final Object lockVehiculos = new Object();

    public VehiculosExposicion(int numeroVehiculos) {
        this.semaforo = new Semaphore(numeroVehiculos, true);
        this.vehiculosOcupados = new boolean[numeroVehiculos];
    }

    public int obtenerVehiculo(String nombreCliente) throws InterruptedException {
        semaforo.acquire();
        int numeroVehiculoAsignado = -1;

        synchronized (lockVehiculos) {
            for (int i = 0; i < vehiculosOcupados.length; i++) {
                if (!vehiculosOcupados[i]) {
                    vehiculosOcupados[i] = true;
                    numeroVehiculoAsignado = i + 1;
                    break;
                }
            }
        }

        if (numeroVehiculoAsignado == -1) {
            semaforo.release();
            throw new IllegalStateException("No se pudo asignar un vehículo al cliente " + nombreCliente);
        }

        System.out.println(nombreCliente + " ... probando vehículo ... " + numeroVehiculoAsignado);

        return numeroVehiculoAsignado;
    }

    public void devolverVehiculo(String nombreCliente, int numeroVehiculo) {
        synchronized (lockVehiculos) {
            int indice = numeroVehiculo - 1;
            if (indice >= 0 && indice < vehiculosOcupados.length && vehiculosOcupados[indice]) {
                vehiculosOcupados[indice] = false;
            }
        }

        System.out.println(nombreCliente + " ... terminó de probar el vehículo ... " + numeroVehiculo);

        semaforo.release();
    }
}

class Cliente extends Thread {

    private final String nombreCliente;
    private final VehiculosExposicion exposicion;
    private final Random random = new Random();

    public Cliente(String nombreCliente, VehiculosExposicion exposicion) {
        this.nombreCliente = nombreCliente;
        this.exposicion = exposicion;
    }

    @Override
    public void run() {
        int numeroVehiculo = -1;
        try {
            numeroVehiculo = exposicion.obtenerVehiculo(nombreCliente);
            int tiempoPrueba = 1000 + random.nextInt(4000);
            Thread.sleep(tiempoPrueba);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (numeroVehiculo != -1) {
                exposicion.devolverVehiculo(nombreCliente, numeroVehiculo);
            }
        }
    }
}
