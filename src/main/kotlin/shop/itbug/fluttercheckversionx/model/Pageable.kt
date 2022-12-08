package shop.itbug.fluttercheckversionx.model

data class Pageable<T>(
    val content: List<T>,
    val empty: Boolean,
    val first: Boolean,
    val last: Boolean,
    val number: Int,
    val numberOfElements: Int,
    val pageable: PageableX,
    val size: Int,
    val sort: SortX,
    val totalElements: Int,
    val totalPages: Int
)