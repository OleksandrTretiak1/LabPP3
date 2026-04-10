import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

class StorageManager {
    private final Semaphore access;
    private final Semaphore emptySpaces;
    private final Semaphore fullSlots;
    private final LinkedList<String> storage;

    public StorageManager(int storageSize) {
        this.access = new Semaphore(1);
        this.emptySpaces = new Semaphore(storageSize);
        this.fullSlots = new Semaphore(0);
        this.storage = new LinkedList<>();
    }

    public void addItem(String item, int producerId) throws InterruptedException {
        emptySpaces.acquire();
        access.acquire();

        storage.add(item);
        System.out.println("[Виробник " + producerId + "] додав " + item + ". Сховище: " + storage.size());

        access.release();
        fullSlots.release();
    }

    public void takeItem(int consumerId) throws InterruptedException {
        fullSlots.acquire();
        access.acquire();

        String item = storage.removeFirst();
        System.out.println("[Споживач " + consumerId + "] взяв " + item + ". Сховище: " + storage.size());

        access.release();
        emptySpaces.release();
    }
}

class Producer implements Runnable {
    private final int id;
    private final int itemsCount;
    private final StorageManager manager;
    private final Semaphore finishSignal;

    public Producer(int id, int itemsCount, StorageManager manager, Semaphore finishSignal) {
        this.id = id;
        this.itemsCount = itemsCount;
        this.manager = manager;
        this.finishSignal = finishSignal;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < itemsCount; i++) {
                String item = "Item_" + id + "_" + i;
                manager.addItem(item, id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finishSignal.release();
        }
    }
}

class Consumer implements Runnable {
    private final int id;
    private final int itemsCount;
    private final StorageManager manager;
    private final Semaphore finishSignal;

    public Consumer(int id, int itemsCount, StorageManager manager, Semaphore finishSignal) {
        this.id = id;
        this.itemsCount = itemsCount;
        this.manager = manager;
        this.finishSignal = finishSignal;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < itemsCount; i++) {
                manager.takeItem(id);
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finishSignal.release();
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введіть розмір сховища: ");
        int storageSize = scanner.nextInt();

        System.out.print("Введіть загальну кількість продукції: ");
        int totalItems = scanner.nextInt();

        System.out.print("Введіть кількість Виробників: ");
        int producersCount = scanner.nextInt();

        System.out.print("Введіть кількість Споживачів: ");
        int consumersCount = scanner.nextInt();

        StorageManager manager = new StorageManager(storageSize);

        Semaphore finishSignal = new Semaphore(0);

        int baseProducerItems = totalItems / producersCount;
        int remainderProducerItems = totalItems % producersCount;

        for (int i = 0; i < producersCount; i++) {
            int itemsToProduce = baseProducerItems + (i < remainderProducerItems ? 1 : 0);
            int producerId = i + 1;
            new Thread(new Producer(producerId, itemsToProduce, manager, finishSignal)).start();
        }

        int baseConsumerItems = totalItems / consumersCount;
        int remainderConsumerItems = totalItems % consumersCount;

        for (int i = 0; i < consumersCount; i++) {
            int itemsToConsume = baseConsumerItems + (i < remainderConsumerItems ? 1 : 0);
            int consumerId = i + 1;
            new Thread(new Consumer(consumerId, itemsToConsume, manager, finishSignal)).start();
        }

        int totalThreads = producersCount + consumersCount;
        for (int i = 0; i < totalThreads; i++) {
            finishSignal.acquire();
        }

        System.out.println("Усі потоки завершили роботу. Програма коректно закривається.");
        scanner.close();
    }
}