package com.d1onyx.core.essentials.paging

/**
 * The token for fetching the next page of data.
 */
public interface PageToken {

    public val value: String

    public companion object {
        public operator fun invoke(value: String): PageToken = PageTokenImpl(value)
    }
}

private data class PageTokenImpl(
    override val value: String,
) : PageToken

/**
 * One page of chunked data, with a link to the [next] page.
 */
public interface PagedData<T> {
    public val data: List<T>
    public val next: PageToken?
}
