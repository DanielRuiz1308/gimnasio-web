package com.gimnasio.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Frena los intentos de adivinar contraseñas: después de 5 fallos seguidos con el mismo
 * usuario desde la misma dirección, se bloquea 5 minutos. Vive en memoria (se borra al reiniciar).
 */
public class IntentosLogin {

    private static final int MAXIMO_FALLOS = 5;
    private static final long BLOQUEO_MS = 5 * 60 * 1000L;

    private static class Estado {
        int fallos;
        long bloqueadoHasta;
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    public boolean bloqueado(String clave) {
        Estado e = estados.get(clave);
        if (e == null) {
            return false;
        }
        if (e.bloqueadoHasta > System.currentTimeMillis()) {
            return true;
        }
        if (e.bloqueadoHasta != 0) { // el bloqueo ya terminó
            estados.remove(clave);
        }
        return false;
    }

    public void fallo(String clave) {
        Estado e = estados.computeIfAbsent(clave, k -> new Estado());
        e.fallos++;
        if (e.fallos >= MAXIMO_FALLOS) {
            e.bloqueadoHasta = System.currentTimeMillis() + BLOQUEO_MS;
        }
    }

    public void exito(String clave) {
        estados.remove(clave);
    }
}