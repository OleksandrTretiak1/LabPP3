import java.util.*
import java.util.concurrent.Semaphore

class StorageManager(storageSize: Int) {
    private val access = Semaphore(1)
    private val emptySpaces = Semaphore(storageSize)
    private val fullSlots = Semaphore(0)
    private val storage = LinkedList<String>()

    @Throws(InterruptedException::class)
    fun addItem(item: String, producerId: Int) {
        emptySpaces.acquire()
        access.acquire()

        storage.add(item)
        println("[Виробник $producerId] додав $item. Сховище: ${storage.size}")

        access.release()
        fullSlots.release()
    }

    @Throws(InterruptedException::class)
    fun takeItem(consumerId: Int) {
        fullSlots.acquire()
        access.acquire()

        val item = storage.removeFirst()
        println("[Споживач $consumerId] взяв $item. Сховище: ${storage.size}")

        access.release()
        emptySpaces.release()
    }
}

class Producer(
    private val id: Int,
    private val itemsCount: Int,
    private val manager: StorageManager
) : Runnable {
    override fun run() {
        try {
            for (i in 0 until itemsCount) {
                val item = "Item_${id}_$i"
                manager.addItem(item, id)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

class Consumer(
    private val id: Int,
    private val itemsCount: Int,
    private val manager: StorageManager
) : Runnable {
    override fun run() {
        try {
            for (i in 0 until itemsCount) {
                manager.takeItem(id)
                Thread.sleep(100)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)

    print("Введіть розмір сховища: ")
    val storageSize = scanner.nextInt()

    print("Введіть загальну кількість продукції: ")
    val totalItems = scanner.nextInt()

    print("Введіть кількість Виробників: ")
    val producersCount = scanner.nextInt()

    print("Введіть кількість Споживачів: ")
    val consumersCount = scanner.nextInt()

    val manager = StorageManager(storageSize)
    val threads = ArrayList<Thread>()

    val baseProducerItems = totalItems / producersCount
    val remainderProducerItems = totalItems % producersCount

    for (i in 0 until producersCount) {
        val itemsToProduce = baseProducerItems + if (i < remainderProducerItems) 1 else 0
        val producerId = i + 1

        val producer = Producer(producerId, itemsToProduce, manager)
        val pThread = Thread(producer)
        threads.add(pThread)
        pThread.start()
    }

    val baseConsumerItems = totalItems / consumersCount
    val remainderConsumerItems = totalItems % consumersCount

    for (i in 0 until consumersCount) {
        val itemsToConsume = baseConsumerItems + if (i < remainderConsumerItems) 1 else 0
        val consumerId = i + 1

        val consumer = Consumer(consumerId, itemsToConsume, manager)
        val cThread = Thread(consumer)
        threads.add(cThread)
        cThread.start()
    }

    for (thread in threads) {
        thread.join()
    }

    println("Усі потоки завершили роботу. Програма коректно закривається.")
    scanner.close()
}