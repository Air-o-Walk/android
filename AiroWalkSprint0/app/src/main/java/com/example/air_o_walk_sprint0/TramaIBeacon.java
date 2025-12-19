package com.example.air_o_walk_sprint0;
import java.util.Arrays;

/**
 * @class TramaIBeacon
 * @brief Representa y parsea la trama de datos de un beacon iBeacon.
 *
 * Esta clase analiza y almacena los diferentes componentes de una trama
 * iBeacon, incluyendo el UUID, major, minor, txPower y otros campos
 * del protocolo. Maneja automáticamente la presencia o ausencia de
 * advertising flags al inicio de la trama.
 *
 * Estructura de la trama iBeacon:
 * - Advertising Flags (3 bytes, opcional)
 * - Advertising Header (2 bytes)
 * - Company ID (2 bytes)
 * - iBeacon Type (1 byte)
 * - iBeacon Length (1 byte)
 * - UUID (16 bytes)
 * - Major (2 bytes)
 * - Minor (2 bytes)
 * - TX Power (1 byte)
 *
 * @author Jordi Bataller i Mascarell y Santiago Aguirre
 * @version 1.0
 */
public class TramaIBeacon {
    private byte[] prefijo = null; // 9 bytes
    private byte[] uuid = null; // 16 bytes
    private byte[] major = null; // 2 bytes
    private byte[] minor = null; // 2 bytes
    private byte txPower = 0; // 1 byte

    private byte[] losBytes;

    private byte[] advFlags = null; // 3 bytes
    private byte[] advHeader = null; // 2 bytes
    private byte[] companyID = new byte[2]; // 2 bytes
    private byte iBeaconType = 0; // 1 byte
    private byte iBeaconLength = 0; // 1 byte

    // --------------------------------------------------------------
    // getPrefijo()
    // Descripción: Obtiene los primeros 9 bytes de la trama que contienen
    //              las flags, header, company ID, type y length.
    // Diseño: getPrefijo() -> prefijo (9 bytes)
    // Retorno: array de bytes con el prefijo de la trama
    // --------------------------------------------------------------
    public byte[] getPrefijo() {
        return prefijo;
    }

    // --------------------------------------------------------------
    // getUUID()
    // Descripción: Obtiene el UUID del beacon (identificador único de 16 bytes).
    // Diseño: getUUID() -> uuid (16 bytes)
    // Retorno: array de bytes con el UUID
    // --------------------------------------------------------------
    public byte[] getUUID() {
        return uuid;
    }

    // --------------------------------------------------------------
    // getMajor()
    // Descripción: Obtiene el valor major del beacon (identificador de grupo, 2 bytes).
    // Diseño: getMajor() -> major (2 bytes)
    // Retorno: array de bytes con el valor major
    // --------------------------------------------------------------
    public byte[] getMajor() {
        return major;
    }

    // --------------------------------------------------------------
    // getMinor()
    // Descripción: Obtiene el valor minor del beacon (identificador individual, 2 bytes).
    // Diseño: getMinor() -> minor (2 bytes)
    // Retorno: array de bytes con el valor minor
    // --------------------------------------------------------------
    public byte[] getMinor() {
        return minor;
    }

    // --------------------------------------------------------------
    // getTxPower()
    // Descripción: Obtiene la potencia de transmisión calibrada del beacon
    //              usada para calcular la distancia.
    // Diseño: getTxPower() -> txPower (1 byte)
    // Retorno: byte con el valor de potencia de transmisión
    // --------------------------------------------------------------
    public byte getTxPower() {
        return txPower;
    }

    // --------------------------------------------------------------
    // getLosBytes()
    // Descripción: Obtiene la trama completa de bytes tal como fue recibida
    //              (incluyendo flags si las tenía o si fueron añadidas).
    // Diseño: getLosBytes() -> losBytes (completo)
    // Retorno: array con todos los bytes de la trama
    // --------------------------------------------------------------
    public byte[] getLosBytes() {
        return losBytes;
    }

    // --------------------------------------------------------------
    // getAdvFlags()
    // Descripción: Obtiene las advertising flags (3 bytes iniciales).
    // Diseño: getAdvFlags() -> advFlags (3 bytes)
    // Retorno: array de bytes con las flags de advertising
    // --------------------------------------------------------------
    public byte[] getAdvFlags() {
        return advFlags;
    }

    // --------------------------------------------------------------
    // getAdvHeader()
    // Descripción: Obtiene el header de advertising (2 bytes).
    // Diseño: getAdvHeader() -> advHeader (2 bytes)
    // Retorno: array de bytes con el header
    // --------------------------------------------------------------
    public byte[] getAdvHeader() {
        return advHeader;
    }

    // --------------------------------------------------------------
    // getCompanyID()
    // Descripción: Obtiene el identificador de la compañía (Apple para iBeacon, 2 bytes).
    // Diseño: getCompanyID() -> companyID (2 bytes)
    // Retorno: array de bytes con el ID de compañía
    // --------------------------------------------------------------
    public byte[] getCompanyID() {
        return companyID;
    }

    // --------------------------------------------------------------
    // getiBeaconType()
    // Descripción: Obtiene el tipo de iBeacon (1 byte).
    // Diseño: getiBeaconType() -> iBeaconType (1 byte)
    // Retorno: byte con el tipo de beacon
    // --------------------------------------------------------------
    public byte getiBeaconType() {
        return iBeaconType;
    }

    // --------------------------------------------------------------
    // getiBeaconLength()
    // Descripción: Obtiene la longitud del payload del iBeacon (1 byte).
    // Diseño: getiBeaconLength() -> iBeaconLength (1 byte)
    // Retorno: byte con la longitud del payload
    // --------------------------------------------------------------
    public byte getiBeaconLength() {
        return iBeaconLength;
    }

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Parsea la trama de bytes recibida y extrae todos los
    //              componentes del iBeacon. Si la trama no contiene
    //              advertising flags al inicio, las añade automáticamente.
    // Diseño: bytes -> TramaIBeacon() -> parsea componentes -> inicializa campos
    // Parámetros: bytes : array de bytes con la trama completa del beacon
    // --------------------------------------------------------------
    public TramaIBeacon(byte[] bytes) {
        // Verificar si la trama ya tiene advertising flags (0x02, 0x01, 0x06)
        if (bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0x02 &&
                (bytes[1] & 0xFF) == 0x01 &&
                (bytes[2] & 0xFF) == 0x06) {
            // Ya tiene flags
            this.losBytes = bytes;
        } else {
            // No tiene flags → añadirlas al inicio
            byte[] flags = new byte[]{0x02, 0x01, 0x06};
            this.losBytes = new byte[flags.length + bytes.length];
            System.arraycopy(flags, 0, this.losBytes, 0, flags.length);
            System.arraycopy(bytes, 0, this.losBytes, flags.length, bytes.length);
        }

        // Extraer componentes de la trama
        prefijo = Arrays.copyOfRange(losBytes, 0, 8 + 1); // 9 bytes
        uuid = Arrays.copyOfRange(losBytes, 9, 24 + 1); // 16 bytes
        major = Arrays.copyOfRange(losBytes, 25, 26 + 1); // 2 bytes
        minor = Arrays.copyOfRange(losBytes, 27, 28 + 1); // 2 bytes
        txPower = losBytes[29]; // 1 byte

        // Extraer sub-componentes del prefijo
        advFlags = Arrays.copyOfRange(prefijo, 0, 2 + 1); // 3 bytes
        advHeader = Arrays.copyOfRange(prefijo, 3, 4 + 1); // 2 bytes
        companyID = Arrays.copyOfRange(prefijo, 5, 6 + 1); // 2 bytes
        iBeaconType = prefijo[7]; // 1 byte
        iBeaconLength = prefijo[8]; // 1 byte

    } // ()
} // class