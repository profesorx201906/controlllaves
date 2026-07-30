package com.institucion.prestamo_llaves_api.shared.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Formato estándar para respuestas paginadas.
 */
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

    public static <S, T> PagedResponse<T> from(
            Page<S> source,
            Function<S, T> mapper
    ) {
        List<T> content = source
            .getContent()
            .stream()
            .map(mapper)
            .toList();

        return new PagedResponse<>(
            content,
            source.getNumber(),
            source.getSize(),
            source.getTotalElements(),
            source.getTotalPages(),
            source.isFirst(),
            source.isLast()
        );
    }
}