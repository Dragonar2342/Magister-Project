package ru.zolotuhin.ModelInfoProcess.Lab4;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetriNet {
    Map<String, Integer> places = new ConcurrentHashMap<>();
    private List<Transition> transitions = new ArrayList<>();
    final int maxReaders;

    public PetriNet(int maxReaders) {
        this.maxReaders = maxReaders;
        initializeNetwork();
    }

    private void initializeNetwork() {
        // Инициализация позиций
        places.put("Free", 1);  // Свободный ресурс
        places.put("Writing", 0); // Процесс записи активен
        places.put("Reading", 0); // Количество активных читателей
        places.put("WaitRead", 0); // Ожидающие читатели
        places.put("WaitWrite", 0); // Ожидающие писатели
        places.put("Mutex", 1); // Мьютекс для синхронизации

        // Инициализация переходов
        transitions.addAll(Arrays.asList(
                // Начало чтения
                new Transition("StartRead",
                        Map.of("Free", 1, "Mutex", 1),
                        Map.of("Reading", 1, "Mutex", 1),
                        Map.of("WaitRead", 0, "Reading", maxReaders)),

                // Конец чтения
                new Transition("EndRead",
                        Map.of("Reading", 1, "Mutex", 1),
                        Map.of("Free", 1, "Mutex", 1),
                        Map.of()),

                // Начало записи
                new Transition("StartWrite",
                        Map.of("Free", 1, "Reading", 0, "Mutex", 1),
                        Map.of("Writing", 1, "Mutex", 1),
                        Map.of()),

                // Конец записи
                new Transition("EndWrite",
                        Map.of("Writing", 1, "Mutex", 1),
                        Map.of("Free", 1, "Mutex", 1),
                        Map.of())
        ));
    }

    public boolean fireTransition(String transitionName) {
        Optional<Transition> transitionOpt = transitions.stream()
                .filter(t -> t.getName().equals(transitionName))
                .findFirst();

        if (transitionOpt.isPresent()) {
            Transition transition = transitionOpt.get();
            if (transition.isEnabled(places)) {
                transition.fire(places);
                System.out.println("Сработал переход: " + transitionName);
                printState();
                return true;
            }
        }
        return false;
    }

    public void printState() {
        System.out.println("Текущее состояние сети:");
        places.forEach((place, tokens) ->
                System.out.println("  " + place + ": " + tokens));
        System.out.println("---");
    }

    public Map<String, Integer> getState() {
        return new HashMap<>(places);
    }

    // Класс перехода
    private static class Transition {
        private String name;
        private Map<String, Integer> inputPlaces;
        private Map<String, Integer> outputPlaces;
        private Map<String, Integer> inhibitors;

        public Transition(String name, Map<String, Integer> inputPlaces,
                          Map<String, Integer> outputPlaces, Map<String, Integer> inhibitors) {
            this.name = name;
            this.inputPlaces = inputPlaces;
            this.outputPlaces = outputPlaces;
            this.inhibitors = inhibitors;
        }

        public boolean isEnabled(Map<String, Integer> currentPlaces) {
            // Проверка входных условий
            for (Map.Entry<String, Integer> entry : inputPlaces.entrySet()) {
                if (currentPlaces.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    return false;
                }
            }

            // Проверка ингибиторных дуг
            for (Map.Entry<String, Integer> entry : inhibitors.entrySet()) {
                if (currentPlaces.getOrDefault(entry.getKey(), 0) >= entry.getValue()) {
                    return false;
                }
            }

            return true;
        }

        public void fire(Map<String, Integer> currentPlaces) {
            // Удаляем метки из входных позиций
            for (Map.Entry<String, Integer> entry : inputPlaces.entrySet()) {
                String place = entry.getKey();
                int tokens = entry.getValue();
                currentPlaces.put(place, currentPlaces.get(place) - tokens);
            }

            // Добавляем метки в выходные позиции
            for (Map.Entry<String, Integer> entry : outputPlaces.entrySet()) {
                String place = entry.getKey();
                int tokens = entry.getValue();
                currentPlaces.put(place, currentPlaces.getOrDefault(place, 0) + tokens);
            }
        }

        public String getName() { return name; }
    }
}
