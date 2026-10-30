package sn.sunuagri.partage.domaine;

import java.util.List;
import java.util.function.Function;

/** Une page de résultats. */
public record PageResultat<T>(List<T> elements, int page, int taille, long totalElements) {

    public int totalPages() {
        return taille == 0 ? 0 : (int) Math.ceil((double) totalElements / taille);
    }

    public <R> PageResultat<R> map(Function<T, R> conversion) {
        return new PageResultat<>(elements.stream().map(conversion).toList(), page, taille, totalElements);
    }
}
