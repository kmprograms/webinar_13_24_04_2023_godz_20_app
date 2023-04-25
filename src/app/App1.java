package app;

import jdk.incubator.concurrent.ScopedValue;
import jdk.incubator.concurrent.StructuredTaskScope;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class App1 {

    /*
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        VIRTUAL THREDS
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        Dlaczego VIRTUAL THREADS?

            -> Java Thread = Operating System Thread
            Wątek systemowy to część programu, która może być zaplanowana i wykonana niezależnie przez
            system operacyjny. Wątki są sposobem osiągania wielozadaniowości w ramach jednego procesu.

            -> W systemie operacyjnym wątek można traktować jako zadanie o mniejszej wadze,
            ponieważ dzieli tę samą przestrzeń pamięci, co inne wątki w ramach tego
            samego procesu. Każdy wątek może wykonywać oddzielne zadania, umożliwiając
            programowi wykonywanie wielu operacji jednocześnie.

            ->Przy okazji ponizej wyjasnienie: wątek vs proces

            Wątek i proces są dwa różne pojęcia, które dotyczą sposobu zarządzania zadaniami przez
            system operacyjny.

            Proces jest instancją programu, która jest uruchamiana i zarządzana przez system operacyjny.
            Każdy proces ma swój własny obszar pamięci oraz listę zasobów systemowych, takich jak pliki i
            połączenia sieciowe. W systemach wieloprocesorowych procesy mogą być przypisywane do różnych
            rdzeni procesora w celu równoległego wykonywania.

            Wątek jest częścią procesu, która może wykonywać się równolegle z innymi wątkami w ramach tego
            samego procesu. Wątki współdzielą obszar pamięci procesu, co oznacza, że mogą one łatwo przekazywać
            dane i komunikować się ze sobą bez konieczności korzystania z mechanizmów systemowych. Wątki są
            lżejsze od procesów, ponieważ nie wymagają tworzenia nowych obszarów pamięci i listy zasobów systemowych,
            jak ma to miejsce w przypadku procesów.

            -> System operacyjny planuje wątki i przydziela zasoby systemowe, takie jak
            czas procesora i pamięć, każdemu wątkowi w miarę potrzeby. Wątki mogą być
            tworzone i zarządzane zarówno przez system operacyjny, jak i przez samą
            aplikację, w zależności od języka programowania i potrzeb programu.

            -> Wątki systemowe to byty, które uznawane są za zasobożerne, dlatego uważa się
            że aplikacja nie powinna mieć więcej niż kilkaset wątków, ponieważ w przeciwnym razie
            prowadzi to do utraty stabilności pracy aplikacji.

            -> Za każdym razem kiedy wątek systemowy jest tworzony, system operacyjny musi zaalokować bardzo dużą
            ilość pamięci na stosie, żeby przechować zasoby wątku. Później podczas pracy wątku, ten ogromny obszar
            pamięci liczony niekiedy w megabajtach, musi być zarządzany. Wiąże się z tym szereg operacji, które są
            kosztowne zarówno pod kątem pamięci, jak również czasu. Duża ilość pamięci zajmowana przez pojedynczy
            wątek systemowy nakłada dodatkowo ograniczenia związane z liczbą możliwych do utworzenia wątków. Przy
            przekroczeniu tej ilości otrzymamy błąd OutOfMemoryError.

            -> Czy kilkaset to dużo czy mało wątków? To zależy od potrzeb aplikacji.
            Dla przykładu jeżeli przetwarzanie requesta (request wymaga utworzenia dla niego osobnego wątku)
            zajmuje 2s, a my z powodów stabilności działania aplikacji ograniczymy thread pool do 100 wątków,
            wtedy na jedną sekunde aplikacja będzie w stanie przetworzyć 50 requestów. Czy to dużo, czy mało?

            -> Poza tym fakt, że utworzymy więcej wątków, nie oznacza jeszcze że nasza aplikacja będzie
            działać szybciej / wydajniej, ponieważ wątek, którym zarządza CPU owszem przetwarza requesta, ale
            w większości przypadków musi czekać, aż przykładowo zakończy się komunikacja z serwisem zewnętrznym
            i to że utworzymy więcej wątków oznaczać będzie, że więcej wątków będzie czekać i tak czy inaczej
            będziemy mieć "przestoje" w pracy mechanizmu wielowątkowości.

            -> Do tej pory z opisanym problemem radziły sobie biblioteki / frameworki takie jak
                -> RxJava (https://github.com/ReactiveX/RxJava)
                -> Project Reactor (https://projectreactor.io/)
                -> CompletableFuture

            Są to rozwiązania powszechnie stosowane, ale niektórzy mają z nimi problem uważając,
            że kod jest nieczytelny, ciężko w debuggowaniu, testowaniu. Jednak zwiększenie wydajności aplikacji,
            które za tym idzie rekomensuje te nie zawsze prawdziwe zarzuty (kwestia wiedzy, umiejętności).

            Ciężko wybronić w tym przypadku problemy z debuggowaniem, ponieważ umieszczenie brekpointów, gdzieś
            w łańcuchu wywołań nic nam nie da, ponieważ całość wykonuje się dopiero, kiedy na końcu wywołasz metodę
            subscribe.

            Inny problem to np nie wszystkie drivers od baz danych czy innych services wspierają reaktywność lub
            to wsparcie jest na poziomie bardzo podstawowym, ciągle rozwijane.
    */

    /*private Mono<GetPlayerDto> createNewPlayerWithTeam(CreatePlayerDto createPlayerDto) {
        return teamDao
                .findByName(createPlayerDto.teamName())
                .flatMap(team -> {
                    var playerToInsert = createPlayerDto.toModel().withTeamId(team.getId());
                    return playerDao
                            .save(playerToInsert)
                            .flatMap(insertedPlayer -> {
                                team.getPlayers().add(insertedPlayer);
                                return teamDao
                                        .save(team)
                                        .flatMap(insertedTeam -> Mono.just(insertedPlayer.toGetPlayerDto()));
                            });
                });
    }*/

    /*
        Rozwiazaniem są VIRTUAL THREADS

        -> Wprowadzenie mechanizmow, dzieki ktorym nie czekasz

        -> Kod tak samo łatwy jak "normalny" kod Java

        -> Z punktu widzenia Java Virtual Thread traktowany jak normalny Thread, ale nie jest on
           mapowany jeden do jednego do system thread.

        -> Tworzony jest specjalny pool (CARRIER). Virtual thread jest mapowany do tego pool-a
           kiedy musi wykonac pewna operacje, ale kiedy Virtual Thread musi poczekac (wtedy mowimy ze wykonuje
           blocking operation) to taki watek jest usuwany z CARRIER THREAD i CARRIER THREAD moze ustawic sie
           na innym VIRTUAL THREAD, ktory w tym czasie wykonuje swoje operacje. Tym nowym watkiem moze byc
           calkiem nowy byt, ktory zaczyna dopiero realizacje swoich zadan, jak rowniez juz istniejacy thread
           ktory wlasnie skonczyl oczekiwanie na wynik operacji blokujacej.

        -> Operacja blokujace przestaja blokowac watki

        -> Minimalizacja czasu wykonania watkow

        -> W jednostce czasu mozesz przetworzyc wiecej watkow

        -> Jeden maly pool moze zarzadzac duza iloscia watkow

        -> Kod latwy w analizie i debuggowaniu (kontynuujesz nawyki nabyte w pisaniu standardowego kodu Java)

        CIEKAWY ARTYKUL NA TEMAT VIRTUAL THREADS OD spring.io
        https://spring.io/blog/2022/10/11/embracing-virtual-threads


    */

    static class AppTask implements Callable<String> {
        private final String message;

        AppTask(String message) {
            this.message = message;
        }

        @Override
        public String call() throws Exception {
            System.out.printf("Start: %s%n", Thread.currentThread().threadId());
            TimeUnit.SECONDS.sleep(3);
            System.out.printf("Stop:  %s%n", Thread.currentThread().threadId());
            return message;
        }
    }

    static void example1() {
        // try (ExecutorService es  = Executors.newFixedThreadPool(10)) {
        // try (ExecutorService es  = Executors.newCachedThreadPool()) {
        try (ExecutorService es  = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = IntStream
                    .range(0, 100)
                    .mapToObj(value -> new AppTask("Task no. " + value))
                    .toList();
            for (var result : es.invokeAll(tasks)) {
                System.out.println(result.get());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void example2() {
        // Sposoby tworzenie wirtualnych watkow
        var t1 = Thread.startVirtualThread(() -> {
            System.out.println("ACTION 1 -> IS VIRTUAL: " + Thread.currentThread().isVirtual());
        });
        var t2 = Thread.ofVirtual().start(() -> System.out.println("ACTION 2"));

        // var t3 = Thread.ofPlatform().start(() -> System.out.println("ACTION 3"));

    }

    /*
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        STRUCTURED CONCURRENCY
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
    */

    /*record Order (int id, String product, int quantity, BigDecimal price) {}

    record Customer(int id, String name, String surname) {}

    record Invoice(Order order, Customer customer, String description) {
        static Invoice generate(Order order, Customer customer, String description) {
            return new Invoice(order, customer, description);
        }
    }

    static class CustomerService {
        private final Map<Integer, Customer> customers = Map.of(
                1, new Customer(1, "ADAM", "NOWAK"),
                2, new Customer(2, "ZOSIA", "KOWAL"),
                3, new Customer(3, "JAN", "KOWALSKI")
        );

        public Customer getById(int id) {
            try {
                TimeUnit.SECONDS.sleep(1);

                if (!customers.containsKey(id)) {
                    throw new IllegalArgumentException("Customer not found");
                }
                return customers.get(id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class OrderService {
        private final Map<Integer, Order> orders = Map.of(
                1, new Order(1, "PROD A", 10, BigDecimal.valueOf(120)),
                2, new Order(2, "PROD B", 20, BigDecimal.valueOf(110)),
                3, new Order(3, "PROD C", 15, BigDecimal.valueOf(140))
        );

        public Order getById(int id) {
            try {
                TimeUnit.SECONDS.sleep(1);

                if (!orders.containsKey(id)) {
                    throw new IllegalArgumentException("Order not found");
                }
                return orders.get(id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }

    static class InvoiceService {
        private final CustomerService customerService;
        private final OrderService orderService;

        InvoiceService(CustomerService customerService, OrderService orderService) {
            this.customerService = customerService;
            this.orderService = orderService;
        }

        String createDescription(String keyword) {
            try {
                if (keyword == null || keyword.isEmpty()) {
                    throw new IllegalArgumentException("Keyword is null or empty");
                }
                TimeUnit.SECONDS.sleep(5);
                return "Invoice description: " + keyword;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public Invoice generateInvoice(int customerId, int orderId, String keyword) {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<Order> orderFuture = executor.submit(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return orderService.getById(orderId);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                Future<Customer> customerFuture = executor.submit(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return customerService.getById(customerId);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                Future<String> invoiceDataFuture = executor.submit(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return createDescription(keyword);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                var order = orderFuture.get();
                var customer = customerFuture.get();
                var invoiceData = invoiceDataFuture.get();

                return Invoice.generate(order, customer, invoiceData);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public Invoice generateInvoice2(int customerId, int orderId, String keyword) {
            // try (var scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                Future<Order> orderFuture = scope.fork(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return orderService.getById(orderId);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                Future<Customer> customerFuture = scope.fork(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return customerService.getById(customerId);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                Future<String> invoiceDataFuture = scope.fork(() -> {
                    try {
                        System.out.println("START: " + Thread.currentThread().threadId());
                        return createDescription(keyword);
                    } finally {
                        System.out.println("STOP:  " + Thread.currentThread().threadId());
                    }
                });

                scope.join();
                scope.throwIfFailed();

                var order = orderFuture.resultNow();
                var customer = customerFuture.resultNow();
                var invoiceData = invoiceDataFuture.resultNow();

                return Invoice.generate(order, customer, invoiceData);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }*/

    /*
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        SCOPED VALUES
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
        ----------------------------------------------------------------------------------------------
    */

    record Order (int id, String product, int quantity, BigDecimal price) {}

    record Customer(int id, String name, String surname) {}

    record Invoice(Order order, Customer customer, String description) {
        static Invoice generate(Order order, Customer customer, String description) {
            return new Invoice(order, customer, description);
        }
    }

    static class CustomerService {
        private final Map<Integer, Customer> customers = Map.of(
                1, new Customer(1, "ADAM", "NOWAK"),
                2, new Customer(2, "ZOSIA", "KOWAL"),
                3, new Customer(3, "JAN", "KOWALSKI")
        );

        public Customer getById(int id) {
            try {
                TimeUnit.SECONDS.sleep(1);

                if (!customers.containsKey(id)) {
                    throw new IllegalArgumentException("Customer not found");
                }
                return customers.get(id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class OrderService {
        private final Map<Integer, Order> orders = Map.of(
                1, new Order(1, "PROD A", 10, BigDecimal.valueOf(120)),
                2, new Order(2, "PROD B", 20, BigDecimal.valueOf(110)),
                3, new Order(3, "PROD C", 15, BigDecimal.valueOf(140))
        );

        public Order getById(int id) {
            try {
                TimeUnit.SECONDS.sleep(1);

                if (!orders.containsKey(id)) {
                    throw new IllegalArgumentException("Order not found");
                }
                return orders.get(id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }

    static class DescriptionService {
        String createDescription(String keyword) {
            try {
                // Tak robisz rebinding

                if (keyword == null || keyword.isEmpty()) {
                    throw new IllegalArgumentException("Keyword is null or empty");
                }
                TimeUnit.SECONDS.sleep(5);
                return InvoiceService.INVOICE_NUMBER.get() + ": Invoice description: " + keyword;

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class InvoiceService {
        private final CustomerService customerService;
        private final OrderService orderService;
        private final DescriptionService descriptionService;

        public static final ScopedValue<String> INVOICE_NUMBER = ScopedValue.newInstance();

        InvoiceService(CustomerService customerService, OrderService orderService, DescriptionService descriptionService) {
            this.customerService = customerService;
            this.orderService = orderService;
            this.descriptionService = descriptionService;
        }

        private static String generateInvoiceNumber() {
            return "FV_%s".formatted(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy_dd_MM_hh_mm_ss")));
        }

        public Invoice generateInvoice2(int customerId, int orderId, String keyword) {
            try {
                return ScopedValue.where(INVOICE_NUMBER, generateInvoiceNumber(), () -> {
                    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                        Future<Order> orderFuture = scope.fork(() -> {
                            try {
                                System.out.println("START: " + Thread.currentThread().threadId());
                                return orderService.getById(orderId);
                            } finally {
                                System.out.println("STOP:  " + Thread.currentThread().threadId());
                            }
                        });

                        Future<Customer> customerFuture = scope.fork(() -> {
                            try {
                                System.out.println("START: " + Thread.currentThread().threadId());
                                return customerService.getById(customerId);
                            } finally {
                                System.out.println("STOP:  " + Thread.currentThread().threadId());
                            }
                        });

                        Future<String> invoiceDataFuture = scope.fork(() -> {
                            try {
                                System.out.println("START: " + Thread.currentThread().threadId());
                                return descriptionService.createDescription(keyword);
                            } finally {
                                System.out.println("STOP:  " + Thread.currentThread().threadId());
                            }
                        });

                        scope.join();
                        scope.throwIfFailed();

                        var order = orderFuture.resultNow();
                        var customer = customerFuture.resultNow();
                        var invoiceData = invoiceDataFuture.resultNow();

                        return Invoice.generate(order, customer, invoiceData);

                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void main(String[] args) {
        var customerService = new CustomerService();
        var orderService = new OrderService();
        var descriptionService = new DescriptionService();
        var invoiceService = new InvoiceService(customerService, orderService, descriptionService);
        // System.out.println(invoiceService.generateInvoice(11, 1, "PON"));
        System.out.println(invoiceService.generateInvoice2(1, 1, "PON"));
    }
}

/*
    Scoped values sa lepsze od ThreadLocals. Maja kilka zalet w stosunku do ThreadLocals:

    -> Dostepne tylko w zakresie ustalonym przez metode where. Po opuszczeniu tego zakresu od razu poddawane
       sa garbage collectingowi.
       ThreadLocal pozostaje w pamieci dopoki watek z nim powiazany istnieje.

    -> Scoped value jest immutable (w gre wchodzi tylko rebinding).

    -> Virtual Threads tworzone w ramach StructuredTaskScope maja dostep do Scoped Values rodzica
       ThreadLocal w takiej sytuacji wykonuje kopiowanie obiekt do subtaska

    Ale pamietaj, ze scoped values moga pracowac rowniez z Platform Threads, jednak mechanizmy, ktorych
    uzywamy same z siebie uzywaja VIRTUAL THREADS.
*/