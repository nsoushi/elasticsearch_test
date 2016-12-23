package nsoushi.elasticsearch

import org.elasticsearch.common.lease.Releasable

/**
 *
 * @author nsoushi
 */
object ReleasableLoan {
    fun <A : Releasable, R> using(s: A, f: (A) -> R): R {
        try {
            return f(s)
        } finally {
            s.close()
        }
    }
}
