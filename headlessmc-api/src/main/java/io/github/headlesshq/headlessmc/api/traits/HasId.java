package io.github.headlesshq.headlessmc.api.traits;

/**
 * A type which can be identified by an id.
 */
public interface HasId {
    static <T extends HasId> T getById(int id, Iterable<T> ids) {
        return getById(String.valueOf(id), ids);
    }

    static <T extends HasId> T getById(String id, Iterable<T> ids) {
        for (T t : ids) {
            if (id.equals(String.valueOf(t.getId()))) {
                return t;
            }
        }

        return null;
    }

    /**
     * @return the id identifying this object.
     */
    int getId();

}
