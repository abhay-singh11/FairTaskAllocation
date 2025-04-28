#!/bin/bash

# Define the parameter sets
fc_values=(0.1 0.3 0.5 0.7 0.9)
p_values=(2 3 5 "inf")

# Loop over instances 1 to 50
for i in $(seq 1 10); do
  for fc in "${fc_values[@]}"; do
    for p in "${p_values[@]}"; do
      # Construct the filename and arguments
      instance="instance_100_20000_${i}.txt"
      args="-n $instance -fc $fc -p $p"
      echo "Running: gradle run --args=\"$args\""
      gradle run --args="$args"

      # Optional: add sleep or output redirection
      # sleep 0.5
      # gradle run --args="$args" >> logs/output_${i}_${fc}_${obj}.log 2>&1
    done
  done
done