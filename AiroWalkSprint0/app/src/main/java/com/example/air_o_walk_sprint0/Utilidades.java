package com.example.air_o_walk_sprint0;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @class Utilidades
 * @brief Clase con métodos estáticos para conversiones entre diferentes tipos de datos.
 *
 * Esta clase proporciona funciones de utilidad para convertir entre
 * strings, bytes, UUIDs, números enteros y otros tipos de datos.
 * Es especialmente útil para trabajar con datos de beacons y
 * comunicaciones de bajo nivel.
 *
 * Funcionalidades principales:
 * - Conversiones entre strings y bytes
 * - Conversiones entre UUID y diferentes representaciones
 * - Conversiones entre arrays de bytes y números (int, long, float)
 * - Formateo hexadecimal de bytes
 *
 * @author Jordi Bataller i Mascarell y Santiago Aguirre
 * @version 1.0
 */
public class Utilidades {

    // --------------------------------------------------------------
    // stringToBytes()
    // Descripción: Convierte un string en un array de bytes usando la codificación por defecto.
    // Diseño: String -> stringToBytes() -> byte[]
    // Parámetros: texto : string a convertir
    // Retorno: array de bytes con el contenido del string
    // --------------------------------------------------------------
    public static byte[] stringToBytes(String texto) {
        return texto.getBytes();
    } // ()

    // --------------------------------------------------------------
    // stringToUUID()
    // Descripción: Convierte un string de exactamente 16 caracteres en un UUID.
    // Diseño: String(16 chars) -> stringToUUID() -> UUID
    // Parámetros: uuid : string de 16 caracteres que representa un UUID
    // Retorno: objeto UUID generado a partir del string
    // Excepción: Error si el string no tiene 16 caracteres
    // --------------------------------------------------------------
    public static UUID stringToUUID(String uuid) {
        if (uuid.length() != 16) {
            throw new Error("stringUUID: string no tiene 16 caracteres ");
        }
        byte[] comoBytes = uuid.getBytes();

        String masSignificativo = uuid.substring(0, 8);
        String menosSignificativo = uuid.substring(8, 16);
        UUID res = new UUID(Utilidades.bytesToLong(masSignificativo.getBytes()),
                Utilidades.bytesToLong(menosSignificativo.getBytes()));

        return res;
    } // ()

    // --------------------------------------------------------------
    // uuidToString()
    // Descripción: Convierte un UUID en su representación como string.
    // Diseño: UUID -> uuidToString() -> String
    // Parámetros: uuid : objeto UUID a convertir
    // Retorno: string con la representación del UUID
    // --------------------------------------------------------------
    public static String uuidToString(UUID uuid) {
        return bytesToString(dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
    } // ()

    // --------------------------------------------------------------
    // uuidToHexString()
    // Descripción: Convierte un UUID en su representación hexadecimal.
    // Diseño: UUID -> uuidToHexString() -> String (hex)
    // Parámetros: uuid : objeto UUID a convertir
    // Retorno: string hexadecimal del UUID
    // --------------------------------------------------------------
    public static String uuidToHexString(UUID uuid) {
        return bytesToHexString(dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
    } // ()

    // --------------------------------------------------------------
    // bytesToString()
    // Descripción: Convierte un array de bytes en un string interpretando
    //              cada byte como un carácter.
    // Diseño: byte[] -> bytesToString() -> String
    // Parámetros: bytes : array de bytes a convertir
    // Retorno: string construido a partir de los bytes
    // --------------------------------------------------------------
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------
    // dosLongToBytes()
    // Descripción: Convierte dos valores long en un array de bytes (16 bytes total).
    //              Útil para manipular UUIDs.
    // Diseño: long + long -> dosLongToBytes() -> byte[16]
    // Parámetros: - masSignificativos : parte más significativa (primeros 8 bytes)
    //             - menosSignificativos : parte menos significativa (últimos 8 bytes)
    // Retorno: array de 16 bytes combinando ambos valores long
    // --------------------------------------------------------------
    public static byte[] dosLongToBytes(long masSignificativos, long menosSignificativos) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES);
        buffer.putLong(masSignificativos);
        buffer.putLong(menosSignificativos);
        return buffer.array();
    }

    // --------------------------------------------------------------
    // bytesToInt()
    // Descripción: Convierte un array de bytes en un valor entero usando BigInteger.
    // Diseño: byte[] -> bytesToInt() -> int
    // Parámetros: bytes : array de bytes a convertir
    // Retorno: valor entero resultante
    // --------------------------------------------------------------
    public static int bytesToInt(byte[] bytes) {
        return new BigInteger(bytes).intValue();
    }

    // --------------------------------------------------------------
    // bytesToFloat()
    // Descripción: Convierte un array de bytes en un valor float usando BigInteger.
    // Diseño: byte[] -> bytesToFloat() -> float
    // Parámetros: bytes : array de bytes a convertir
    // Retorno: valor float resultante
    // --------------------------------------------------------------
    public static float bytesToFloat(byte[] bytes) {
        return new BigInteger(bytes).floatValue();
    }

    // --------------------------------------------------------------
    // bytesToLong()
    // Descripción: Convierte un array de bytes en un valor long usando BigInteger.
    // Diseño: byte[] -> bytesToLong() -> long
    // Parámetros: bytes : array de bytes a convertir
    // Retorno: valor long resultante
    // --------------------------------------------------------------
    public static long bytesToLong(byte[] bytes) {
        return new BigInteger(bytes).longValue();
    }

    // --------------------------------------------------------------
    // bytesToIntOK()
    // Descripción: Convierte un array de bytes en un entero de forma manual,
    //              manejando correctamente el signo (complemento a 2).
    // Diseño: byte[] -> bytesToIntOK() -> int con signo
    // Parámetros: bytes : array de bytes a convertir (máximo 4 bytes)
    // Retorno: valor entero con signo resultante
    // Excepción: Error si el array tiene más de 4 bytes
    // --------------------------------------------------------------
    public static int bytesToIntOK(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }

        if (bytes.length > 4) {
            throw new Error("demasiados bytes para pasar a int ");
        }
        int res = 0;

        for (byte b : bytes) {
            res = (res << 8) // Desplaza 8 bits a la izquierda
                    + (b & 0xFF); // Añade el byte actual (sin signo)
        } // for

        // Manejo del signo (complemento a 2)
        if ((bytes[0] & 0x8) != 0) {
            // Si tiene signo negativo (bit más significativo = 1)
            res = -(~(byte) res) - 1; // Complemento a 2
        }

        return res;
    } // ()

    // --------------------------------------------------------------
    // bytesToHexString()
    // Descripción: Convierte un array de bytes en una representación hexadecimal
    //              legible, separando cada byte con dos puntos.
    // Diseño: byte[] -> bytesToHexString() -> String "XX:XX:XX..."
    // Parámetros: bytes : array de bytes a convertir
    // Retorno: string hexadecimal formateado
    // --------------------------------------------------------------
    public static String bytesToHexString(byte[] bytes) {

        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
            sb.append(':');
        }
        return sb.toString();
    } // ()
} // class