package net.b2net.utils.iot.server;

/**
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS
 * OF THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 * <p/>
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS
 * RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY
 * CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR
 * DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON, AND/OR
 * DERIVED FROM THIS SOURCE CODE FILE.
 */
final class DeleteList
{
    private DeleteListElement first = null;
    private DeleteListElement last = null;
    private int lastBufferSize = 0;
    private int lastMessageCount = 0;

    void incLastBufferSize(int size)
    {
        lastBufferSize += size;
    }

    void incLastMessageCount()
    {
        lastMessageCount++;
    }

    void clearLast()
    {
        lastBufferSize = 0;
        lastMessageCount = 0;
    }

    int getLastBufferSize()
    {
        return lastBufferSize;
    }

    int getLastMessageCount()
    {
        return lastMessageCount;
    }

    void push(DeleteListElement element)
    {
        if (last != null)
        {
            element.prev = last;
            last.next = element;
            last = element;
        }
        else
        {
            first = element;
            last = element;
        }
    }

    void delete(DeleteListElement element)
    {
        if (element.prev != null)
        {
            element.prev.next = element.next;
        }

        if (element.next != null)
        {
            element.next.prev = element.prev;
        }

        if (first == element)
        {
            first = element.next;
        }

        if (last == element)
        {
            last = element.prev;
        }

        element.prev = null;
        element.next = null;
    }


    DeleteListElement pop()
    {
        if (first == null)
        {
            return null;
        }

        DeleteListElement ret = first;

        delete(ret);

        return ret;
    }

}
