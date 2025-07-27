import sqlite3, logging, argparse, ast
import csv, os, numpy as np
from math import ceil, floor

log = logging.getLogger(__name__)

class ScriptException(Exception):
    """Custom exception class with message for this module."""

    def __init__(self, value):
        self.value = value
        super().__init__(value)

    def __repr__(self):
        return repr(self.value)


class databaseToCSV():
    def __init__(self, database_path, results_path) -> None:
        self.connection = sqlite3.connect(database_path)
        self.cursor = self.connection.cursor()
        self.results_path = results_path

    def _closeConnection(self):                
        self.cursor.close()
        self.connection.close()

    def _getTotalCost(self, numSource, numTarget, instance_name, pNorm, fc):
        self.cursor.execute(f"""
            SELECT TotalCost
            FROM {"instance_" + str(numSource) + "_" + str(numTarget)}
            WHERE instanceName = ? AND pNorm = ? AND fairnessCoefficient = ?
            """, (instance_name, pNorm, str(fc) ))
        result = self.cursor.fetchone()
        if result is None:
            raise ScriptException(f"Data not found for instance: {instance_name}, pNorm: {pNorm}, fc: {fc}")
        return float(result[0])
    
    def _getCostList(self, numSource, numTarget, instance_name, pNorm, fc):
        self.cursor.execute(f"""
            SELECT AssignmentCost
            FROM {"instance_" + str(numSource) + "_" + str(numTarget)}
            WHERE instanceName = ? AND pNorm = ? AND fairnessCoefficient = ?
            """, (instance_name, pNorm, str(fc) ))
        result = self.cursor.fetchone()
        if result is None:
            raise ScriptException(f"Data not found for instance: {instance_name}, pNorm: {pNorm}, fc: {fc}")
        else:
            result = [round(abs(len),2) for len in ast.literal_eval(result[0])]
        return result
    
    def _getComputationTime(self, numSource, numTarget, instance_name, pNorm, fc):
        self.cursor.execute(f"""
            SELECT computationTimeInSec
            FROM {"instance_" + str(numSource) + "_" + str(numTarget)}
            WHERE instanceName = ? AND pNorm = ? AND fairnessCoefficient = ?
            """, (instance_name, pNorm, str(fc) ))
        result = self.cursor.fetchone()
        if result is None:
            raise ScriptException(f"Computation time data not found for instance: {instance_name}, pNorm: {pNorm}, fc: {fc}")
        return float(result[0])

    def paretoFrontData(self, tableName, instance_name='instance_10_500_1.txt'):

        csv_filename = os.path.join(self.results_path, f'{tableName+"_"+instance_name.split(".")[0]}.csv')
        numSource = instance_name.split('_')[1]
        numTarget = instance_name.split('_')[2]

        with open(csv_filename, 'w', newline='') as csvfile:
            csv_writer = csv.writer(csvfile)
            csv_writer.writerow(['instance_name', instance_name])
            pNorms = ["2", "3", "5", "10", "inf"]
            fairnessCoefficient = [round(x,2) for x in np.arange(0.1,1.0,0.1)] + [0.99]
            csv_writer.writerow(["fairnessCoefficient"] + fairnessCoefficient)
            
            for p in pNorms:
                data = ["pNorm: " + p]
                for fc in fairnessCoefficient:
                    cost = self._getTotalCost(numSource=numSource, numTarget=numTarget, instance_name=instance_name, pNorm=p, fc=fc)
                    data.append(round(cost, 2))
                csv_writer.writerow(data)

    def COVData(self, tableName, instance_name='instance_10_500_1.txt'):
        csv_filename = os.path.join(self.results_path, f'{tableName+"_"+instance_name.split(".")[0]}.csv')
        numSource = instance_name.split('_')[1]
        numTarget = instance_name.split('_')[2]

        with open(csv_filename, 'w', newline='') as csvfile:
            csv_writer = csv.writer(csvfile)
            csv_writer.writerow(['instance_name', instance_name])
            pNorms = ["2", "3", "5", "10", "inf"]
            fairnessCoefficient = [round(x,2) for x in np.arange(0.1,1.0,0.1)] + [0.99]
            csv_writer.writerow(["fairnessCoefficient"] + fairnessCoefficient)
            
            for p in pNorms:
                data = ["pNorm: " + p]
                for fc in fairnessCoefficient:
                    cost = self._getCostList(numSource=numSource, numTarget=numTarget, instance_name=instance_name, pNorm=p, fc=fc)
                    COV = np.std(cost) / np.mean(cost)
                    data.append(round(COV, 2))
                csv_writer.writerow(data)

    def computationTimeData(self, tableName, numSources=5, numTargets=100):
        csv_filename = os.path.join(self.results_path, f'{tableName}_instance_{numSources}_{numTargets}.csv')
        numSource = numSources
        numTarget = numTargets

        with open(csv_filename, 'w', newline='') as csvfile:
            csv_writer = csv.writer(csvfile)
            csv_writer.writerow(['numSources', f'{numSources}'])
            csv_writer.writerow(['numTargets', f'{numTargets}'])
            pNorms = ["2", "3", "5", "10", "inf"]
            fairnessCoefficient = [round(x,2) for x in np.arange(0.1,1.0,0.2)] + [0.99]
            csv_writer.writerow(["pNorm", "fairnessCoefficient"] + [f'comp_time_instance_{i}' for i in range(1, 51)])

            for p in pNorms:
                for fc in fairnessCoefficient:
                    row = [p, fc]
                    for instance in range(1, 51):
                        instance_name = f'instance_{numSources}_{numTargets}_{instance}.txt'
                        computationTime = self._getComputationTime(numSource=numSource, numTarget=numTarget, instance_name=instance_name, pNorm=p, fc=fc)
                        row.append(round(computationTime, 2))
                    csv_writer.writerow(row)




def handle_command_line():
    parser = argparse.ArgumentParser()

    parser.add_argument("-tableName", "--tableName", choices=['paretoFront', 'COV', 'computationTime'],
                        help="give the table name", type=str)
    parser.add_argument("-s", "--sources", type=int, default=5,
                        help="number of sources in the instance (default: 5)")
    parser.add_argument("-t", "--targets", type=int, default=100,
                        help="number of targets in the instance (default: 100)")
    parser.add_argument("-i", "--instance", help="give the instance number", type=int, default=1)
    
    args = parser.parse_args()

    return args.tableName, args.sources, args.targets, args.instance


def main():
    logging.basicConfig(format='%(asctime)s %(levelname)s--: %(message)s',
                        level=logging.DEBUG)

    try:
        folder_path = os.path.dirname(os.path.realpath(__file__))
        base_path = os.path.abspath(os.path.join(folder_path, '..'))
        results_path = os.path.join(base_path, 'data/results/')
        db_path = os.path.join(base_path, 'data/results.db')

        tableName, numSources, numTargets, instance = handle_command_line()
        instanceName = f'instance_{numSources}_{numTargets}_{instance}.txt'
        dataTransfer = databaseToCSV(db_path, results_path)
        
        if tableName == 'paretoFront':
            dataTransfer.paretoFrontData(tableName, instanceName)
        elif tableName == 'COV':
            dataTransfer.COVData(tableName, instanceName)
        elif tableName == 'computationTime':
            dataTransfer.computationTimeData(tableName, numSources, numTargets)

        dataTransfer._closeConnection()
    
    
    except ScriptException as se:
        log.error(se)


if __name__ == '__main__':
    main()
