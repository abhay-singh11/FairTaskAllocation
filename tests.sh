#!/bin/bash

# Define the parameter sets
fc_values=(0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 0.99)
#fc_values=(0.99)
p_values=(2 3 5 10 "inf")
numSource=5
numTarget=250

# Loop over instances 1 to 50
for i in $(seq 6 10); do
  for fc in "${fc_values[@]}"; do
    for p in "${p_values[@]}"; do
      # Construct the filename and arguments
      instance="instance_${numSource}_${numTarget}_${i}.txt"
      args="-s $numSource -t $numTarget -n $instance -fc $fc -p $p -time 900"
      echo "Running: gradle run --args=\"$args\""
      gradle run --args="$args"

      # Optional: add sleep or output redirection
      # sleep 0.5
      # gradle run --args="$args" >> logs/output_${i}_${fc}_${obj}.log 2>&1
    done
  done
done