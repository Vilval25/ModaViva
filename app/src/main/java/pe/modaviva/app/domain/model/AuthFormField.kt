package pe.modaviva.app.domain.model

/**
 * Campos de los formularios de autenticación. Vive en el dominio porque los
 * use cases de validación devuelven errores indexados por campo, y la capa de
 * datos no debe depender de la UI.
 */
enum class AuthFormField {
    NOMBRES,
    APELLIDOS,
    DOCUMENTO,
    TELEFONO,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD,
}
