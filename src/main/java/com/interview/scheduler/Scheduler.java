package com.interview.scheduler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Integer.valueOf;
import static java.util.Collections.singletonList;

public class Scheduler {

    private static final String ANY = "*";
    private static final List<Integer> ALL_MINS = IntStream.iterate(0, i -> i + 1).limit(60).boxed().collect(Collectors.toList());
    private static final List<Integer> ALL_HOURS = IntStream.iterate(0, i -> i + 1).limit(24).boxed().collect(Collectors.toList());
    private static final int TOMORROW_DAY_INCREASE = 1;
    private static final int EARLY_CRON_JOB_TIME = 0;
    private static final String HH_MM_PATTERN = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$";

    public static void main(String[] args) throws ParseException {
        validateInputs(args);

        Date currentTime = initCurrentTime(args[0].split(":")[0], args[0].split(":")[1]);
        String configPath = System.getProperty("user.dir") + "\\" + args[1];

        List<CronJob> cronJobs = initCronJobs(configPath);
        cronJobs.forEach(job -> printClosestJobTime(job, currentTime));
    }

    private static void printClosestJobTime(CronJob cronJob, Date currentTime) {
        Date closestDate = cronJob.getClosestTime(currentTime);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        boolean isTheSameDay = currentTime.getDay() == closestDate.getDay();
        String day = isTheSameDay ? " today " : " tomorrow ";
        System.out.println(formatter.format(closestDate) + day  + cronJob.getJobName());
    }

    private static List<CronJob> initCronJobs(String configPath) {
        try {
            Stream<String> stream = Files.lines(Paths.get(configPath));
            return stream.map(configRecord -> {
                String[] configRecordData = configRecord.split(" ");
                if (configRecordData.length != 3) throw new IllegalArgumentException(configRecord);
                String minutesPattern = configRecordData[0];
                String hoursPattern = configRecordData[1];
                String jobName = configRecordData[2];
                return new CronJob(jobName, minutesPattern, hoursPattern);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Wrong record in configuration file: " + e.getMessage());
        }
    }

    private static void validateInputs(String[] args) {
        if(args.length < 2) throw new RuntimeException("Please specify current time HH:MM and config file");
        String timeArgument = args[0];
        String pathArgument = args[1];
        if (!isValidHHMMPattern(timeArgument))
            throw new RuntimeException("Current time argument: " + timeArgument + " is not following HH:MM pattern!");
        File file = new File(System.getProperty("user.dir") + "\\" + pathArgument);
        if (!file.exists()) throw new RuntimeException("Config file path : " + pathArgument + " doesn't exist!");
    }

    private static Date initCurrentTime(String hours, String mins) {
        return generateCalendar(valueOf(hours), valueOf(mins)).getTime();
    }

    private static class CronJob {
        private String jobName;
        private List<Date> scheduledTimeList = new ArrayList<>();

        CronJob(String jobName, String minutePattern, String hourPattern) {
            List<Integer> minutes = ANY.equals(minutePattern) ? ALL_MINS : singletonList(valueOf(minutePattern));
            List<Integer> hours = ANY.equals(hourPattern) ? ALL_HOURS : singletonList(valueOf(hourPattern));
            this.jobName = jobName;
            for (Integer hour : hours) {
                for (Integer minute : minutes) {
                    scheduledTimeList.add(generateCalendar(hour, minute).getTime());
                }
            }
        }

        public String getJobName() {
            return jobName;
        }

        Date getClosestTime(Date currentTime) {
            return getClosestCronJobTodayTime(currentTime).orElse(getClosestTomorrowTime());

        }

        private Optional<Date> getClosestCronJobTodayTime(Date currentTime) {
            for (Date scheduleTime : scheduledTimeList) {
                if (currentTime.compareTo(scheduleTime) <= 0) {
                    return Optional.of(scheduleTime);
                }
            }
            return Optional.empty();
        }

        private Date getClosestTomorrowTime() {
            Calendar firstTomorrowCalendarTime = Calendar.getInstance();
            firstTomorrowCalendarTime.setTime(scheduledTimeList.get(EARLY_CRON_JOB_TIME));
            firstTomorrowCalendarTime.add(Calendar.DATE, TOMORROW_DAY_INCREASE);
            return firstTomorrowCalendarTime.getTime();
        }
    }

    private static boolean isValidHHMMPattern(String currentTimeArgument) {
        return Pattern.compile(HH_MM_PATTERN).matcher(currentTimeArgument).matches();
    }

    private static Calendar generateCalendar(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

}
