package de.soderer.utilities.swt;

import java.time.Duration;

public class WorkerStats {
	private final int workerId;
	private int successCount;
	private int errorCount;
	private Duration minimumDuration;
	private Duration maximumDuration;
	private Duration latestDuration;
	private Boolean latestStatusWasSuccess = null;
	private Duration averageExecutionDuration;
	private long durationOverallMillis;
	private int numberOfExecutions;

	public WorkerStats(final int workerId) {
		this.workerId = workerId;
	}

	public synchronized void addSuccess(final Duration duration) {
		successCount++;
		updateDauer(duration);
		latestStatusWasSuccess = true;
	}

	public synchronized void addError(final Duration duration) {
		errorCount++;
		updateDauer(duration);
		latestStatusWasSuccess = false;
	}

	private void updateDauer(final Duration duration) {
		latestDuration = duration;
		durationOverallMillis += duration.toMillis();
		numberOfExecutions++;

		if (minimumDuration == null) {
			minimumDuration = duration;
		} else {
			minimumDuration = (duration.compareTo(minimumDuration) >= 0) ? minimumDuration : duration;
		}

		averageExecutionDuration = Duration.ofMillis(durationOverallMillis / numberOfExecutions);

		if (maximumDuration == null) {
			maximumDuration = duration;
		} else {
			maximumDuration = (duration.compareTo(maximumDuration) >= 0) ? duration : maximumDuration;
		}
	}

	public int getWorkerId() {
		return workerId;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public Duration getMinimumDuration() {
		return minimumDuration;
	}

	public Duration getAverageDuration() {
		return averageExecutionDuration;
	}

	public Duration getMaximumDuration() {
		return maximumDuration;
	}

	public Duration getLatestDuration() {
		return latestDuration;
	}

	public Boolean getLatestStatusWasSuccess() {
		return latestStatusWasSuccess;
	}
}