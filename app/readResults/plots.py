import csv, os
import matplotlib
import matplotlib.pyplot as plt
import argparse, logging, subprocess


log = logging.getLogger(__name__)

class PlotException(Exception):
    """Custom exception class with message for this module."""

    def __init__(self, value):
        self.value = value
        super().__init__(value)

    def __repr__(self):
        return repr(self.value)


def handle_command_line():
    """function to manage command line arguments """
    parser = argparse.ArgumentParser() 
    parser.add_argument('-t', '--target', choices=['paper', 'presentation'], 
                        help='paper/presentation', type=str)
    parser.add_argument('-g', '--plotgrid', action='store_true',
                        help='generate the grid plot')
    parser.add_argument('-c', '--pdfcrop', action='store_true', 
                        help='crop figures after generating them')
    parser.add_argument('-o', '--option', choices=['paretoFront', 'COV'],
                        help='plot paretoFront or COV', type=str)
    parser.add_argument('-i', '--instanceName', type=str, default='instance_10_500_1',
                        help='provide instance name')
    config = parser.parse_args()
    return config


class Controller: 
    """class that manages the functionality of the entire plotting script"""
    
    def __init__(self, config):
        self.config = config

    @staticmethod
    def get_plot_params(target):
        if target == 'paper':
            return {
                'fig_size': (2.5, 2.5)
            }
        else: 
            return {
                'fig_size': (4, 3)
            }
        

    @staticmethod
    def getbasepath():
        return os.path.abspath(os.path.join(os.path.dirname(__file__) , '..'))
    
    @staticmethod
    def getDataFilepath(basepath, instance_name, option='paretoFront'):
        if option == 'paretoFront':
            return os.path.join(basepath, 'data', 'results', 'paretoFront_'+instance_name+'.csv')
        elif option == 'COV':
            return os.path.join(basepath, 'data', 'results', 'COV_'+instance_name+'.csv')
        else:
            log.error(f"Invalid option: {option}")
            raise PlotException(f"Invalid option: {option}")
    
    @staticmethod 
    def crop(file): 
        log.info(f'cropping figure {file}')
        subprocess.run(['pdfcrop', f'{file}.pdf'])
        subprocess.run(['rm', '-rf', f'{file}.pdf'])
        subprocess.run(['mv', f'{file}-crop.pdf', f'{file}.pdf'])
        log.info(f'cropping figure {file} complete')
    
    def _set_rc_params(self):
        if self.config.target == 'paper':
            matplotlib.rcParams.update({
                'text.usetex': True,
                'font.family': 'serif',
                'font.size' : 10,
                'pgf.rcfonts': False,
                })
        # else: 
        #     matplotlib.rcParams.update({
        #         'text.usetex': True,
        #         'font.family': 'sans-serif',
        #         'text.latex.preamble': r'\usepackage{sourcesanspro,eulervm}',
        #         'font.size' : 11,
        #         'pgf.rcfonts': False,
        #         })

    def getParetoFrontData(self):
        basepath = self.getbasepath()
        dataFilepath = self.getDataFilepath(basepath, self.config.instanceName)
        with open(dataFilepath, 'r') as csvfile:
            reader = csv.reader(csvfile)
            paretoFrontData = {}
            fairnessCoefficient = []
            for row in reader:
                if row[0] == 'fairnessCoefficient':
                    fairnessCoefficient = [float(x) for x in row[1:]]
                elif row[0].split(":")[0] == 'pNorm':
                    paretoFrontData[row[0]] = [float(x) for x in row[1:]]
        
        optimalCost = min([min(data) for data in paretoFrontData.values()])
        for pNorm, data in paretoFrontData.items():
            paretoFrontData[pNorm] = [round((x - optimalCost) / optimalCost, 5) for x in data]
        
        
        return paretoFrontData, fairnessCoefficient
    
    def getCOVData(self):
        basepath = self.getbasepath()
        dataFilepath = self.getDataFilepath(basepath, self.config.instanceName, self.config.option)
        with open(dataFilepath, 'r') as csvfile:
            reader = csv.reader(csvfile)
            COVData = {}
            fairnessCoefficient = []
            for row in reader:
                if row[0] == 'fairnessCoefficient':
                    fairnessCoefficient = [float(x) for x in row[1:]]
                elif row[0].split(":")[0] == 'pNorm':
                    COVData[row[0]] = [float(x) for x in row[1:]]
        
        return COVData, fairnessCoefficient
        

    def plotParetoFront(self):
        paretoFronts, fairnessCoefficient = self.getParetoFrontData()

        fig, ax = plt.subplots()
        params = self.get_plot_params(self.config.target)
        fs=params['fig_size']
        # fig.set_size_inches(fs[0], fs[1])
        fairnessCoefficient = [1-x for x in fairnessCoefficient]
        fig.set_size_inches(6,4)
        for pNorm, data in paretoFronts.items():
            ax.plot(fairnessCoefficient, data, label=pNorm)
        
        plt.xlabel(r'$1-\varepsilon$', fontsize = 12)
        plt.ylabel('Cost of Fairness (CoF)', fontsize = 12)
        plt.legend(loc='upper right')
        plt.tight_layout()

        plt.savefig(f'plots/{self.config.target}/paretoFront_{self.config.instanceName}.pdf', format='pdf')

    def plotCOV(self):
        COVData, fairnessCoefficient = self.getCOVData()

        fig, ax = plt.subplots()
        params = self.get_plot_params(self.config.target)
        fs=params['fig_size']
        # fig.set_size_inches(fs[0], fs[1])
        fig.set_size_inches(6,4)
        for pNorm, data in COVData.items():
            ax.plot(fairnessCoefficient, data, label=pNorm)
        
        plt.xlabel(r'$\varepsilon$', fontsize = 12)
        plt.ylabel('Coefficient of Variation (CoV)', fontsize = 12)
        plt.legend(loc='upper right')
        plt.tight_layout()

        plt.savefig(f'plots/{self.config.target}/COV_{self.config.instanceName}.pdf', format='pdf')
        
    def plot(self):
        if self.config.option == 'paretoFront':
            self.plotParetoFront()
        elif self.config.option == 'COV':
            self.plotCOV()
        else:
            raise PlotException(f"Invalid option: {self.config.option}")
        
        if self.config.pdfcrop:
            self.crop(f'plots/{self.config.target}/{self.config.option}_{self.config.instanceName}')


def main():
    logging.basicConfig(format='%(asctime)s %(levelname)s--: %(message)s',
                        level=logging.INFO)
    
    try:
        config = handle_command_line()
        controller = Controller(config)
        controller._set_rc_params()
        controller.plot()
    except PlotException as pe:
        log.error(pe)

if __name__ == "__main__":
    main()