package com.institucion.prestamo_llaves_api.shared.exception;


/**
 * Se utiliza cuando un recurso solicitado no existe.
 */
public class ResourceNotFoundException
        extends ApplicationException {

    public ResourceNotFoundException(
            String resourceName,
            Object resourceId
    ) {
        super(
            "RESOURCE_NOT_FOUND",
            resourceName
                + " no encontrado con identificador "
                + resourceId
        );
    }
}