using System;
using System.Collections.Generic;
using System.Threading;

namespace ParallelPLab3
{
    public class StorageManager
    {
        private readonly Semaphore _access;
        private readonly Semaphore _emptySpaces;
        private readonly Semaphore _fullSlots;
        private readonly List<string> _storage;

        public StorageManager(int storageSize)
        {
            _access = new Semaphore(1, 1);
            _emptySpaces = new Semaphore(storageSize, storageSize);
            _fullSlots = new Semaphore(0, storageSize);
            _storage = new List<string>();
        }

        public void AddItem(string item, int producerId)
        {
            _emptySpaces.WaitOne();
            _access.WaitOne();

            _storage.Add(item);
            Console.WriteLine($"[Виробник {producerId}] додав {item}. Сховище: {_storage.Count}");

            _access.Release();
            _fullSlots.Release();
        }

        public void TakeItem(int consumerId)
        {
            _fullSlots.WaitOne();
            _access.WaitOne();

            string item = _storage[0];
            _storage.RemoveAt(0);
            Console.WriteLine($"[Споживач {consumerId}] взяв {item}. Сховище: {_storage.Count}");

            _access.Release();
            _emptySpaces.Release();
        }
    }

    public class Producer
    {
        private readonly int _id;
        private readonly int _itemsCount;
        private readonly StorageManager _manager;
        private readonly Semaphore _finishSignal;

        public Producer(int id, int itemsCount, StorageManager manager, Semaphore finishSignal)
        {
            _id = id;
            _itemsCount = itemsCount;
            _manager = manager;
            _finishSignal = finishSignal;
        }

        public void Run()
        {
            for (int i = 0; i < _itemsCount; i++)
            {
                string item = $"Item_{_id}_{i + 1}";
                _manager.AddItem(item, _id);
            }
            _finishSignal.Release();
        }
    }

    public class Consumer
    {
        private readonly int _id;
        private readonly int _itemsCount;
        private readonly StorageManager _manager;
        private readonly Semaphore _finishSignal;

        public Consumer(int id, int itemsCount, StorageManager manager, Semaphore finishSignal)
        {
            _id = id;
            _itemsCount = itemsCount;
            _manager = manager;
            _finishSignal = finishSignal;
        }

        public void Run()
        {
            for (int i = 0; i < _itemsCount; i++)
            {
                _manager.TakeItem(_id);
                Thread.Sleep(1000);
            }
            _finishSignal.Release();
        }
    }

    class Program
    {
        static void Main(string[] args)
        {
            Console.OutputEncoding = System.Text.Encoding.UTF8;

            Console.WriteLine("=== Параметри системи ===");
            Console.Write("Введіть розмір сховища: ");
            int storageSize = int.Parse(Console.ReadLine() ?? "5");

            Console.Write("Введіть загальну кількість продукції: ");
            int totalItems = int.Parse(Console.ReadLine() ?? "20");

            Console.Write("Введіть кількість Виробників: ");
            int producersCount = int.Parse(Console.ReadLine() ?? "2");

            Console.Write("Введіть кількість Споживачів: ");
            int consumersCount = int.Parse(Console.ReadLine() ?? "2");
            Console.WriteLine("--------------------------\n");

            StorageManager manager = new StorageManager(storageSize);

            Semaphore finishSignal = new Semaphore(0, producersCount + consumersCount);

            int baseProducerItems = totalItems / producersCount;
            int remainderProducerItems = totalItems % producersCount;

            for (int i = 0; i < producersCount; i++)
            {
                int itemsToProduce = baseProducerItems + (i < remainderProducerItems ? 1 : 0);
                Producer producer = new Producer(i + 1, itemsToProduce, manager, finishSignal);
                new Thread(producer.Run).Start();
            }

            int baseConsumerItems = totalItems / consumersCount;
            int remainderConsumerItems = totalItems % consumersCount;

            for (int i = 0; i < consumersCount; i++)
            {
                int itemsToConsume = baseConsumerItems + (i < remainderConsumerItems ? 1 : 0);
                Consumer consumer = new Consumer(i + 1, itemsToConsume, manager, finishSignal);
                new Thread(consumer.Run).Start();
            }

            for (int i = 0; i < (producersCount + consumersCount); i++)
            {
                finishSignal.WaitOne();
            }

            Console.WriteLine("\n[Успіх] Усі потоки завершили роботу. Сховище пусте.");
            Console.WriteLine("Натисніть будь-яку клавішу для виходу...");
            Console.ReadKey();
        }
    }
}